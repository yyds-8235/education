package com.example.education.service;

import com.example.education.common.exception.BusinessException;
import com.example.education.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter COURSE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "webm");
    private static final Set<String> PPT_EXTENSIONS = Set.of("ppt", "pptx");
    private static final Set<String> WORD_EXTENSIONS = Set.of("doc", "docx");
    private static final Set<String> PDF_EXTENSIONS = Set.of("pdf");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp");

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public UploadResult uploadAvatar(MultipartFile file, String userId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(422, "上传文件不能为空");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(422, "仅支持图片文件");
        }

        String objectKey = buildAvatarObjectKey(file.getOriginalFilename(), userId);
        try (InputStream inputStream = file.getInputStream()) {
            ensureBucketExists(minioProperties.getBucket());
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .contentType(contentType)
                            .stream(inputStream, file.getSize(), -1)
                            .build());
            return new UploadResult(objectKey, buildObjectUrl(objectKey));
        } catch (Exception ex) {
            throw new BusinessException(500, "头像上传失败");
        }
    }

    public CourseResourceUploadResult uploadCourseResource(MultipartFile file, String teacherId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(422, "上传文件不能为空");
        }

        String originalFilename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "resource.bin";
        String type = resolveCourseResourceType(getExtension(originalFilename));
        String objectKey = buildCourseObjectKey(originalFilename, teacherId);
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";

        try (InputStream inputStream = file.getInputStream()) {
            ensureBucketExists(minioProperties.getBucket());
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .object(objectKey)
                            .contentType(contentType)
                            .stream(inputStream, file.getSize(), -1)
                            .build());
            return new CourseResourceUploadResult(
                    minioProperties.getBucket(),
                    objectKey,
                    buildObjectUrl(objectKey),
                    originalFilename,
                    type,
                    file.getSize());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(500, "课件上传失败");
        }
    }

    public String generatePreviewUrl(String bucketName,
                                     String objectKey,
                                     String fileName,
                                     String resourceType,
                                     int expiresInSeconds) {
        ensureObjectExists(bucketName, objectKey);
        Map<String, String> queryParams = new LinkedHashMap<>();
        if (isPreviewInlineType(resourceType)) {
            queryParams.put("response-content-disposition", buildDisposition("inline", fileName));
            String contentType = resolvePreviewContentType(resourceType, fileName);
            if (StringUtils.hasText(contentType)) {
                queryParams.put("response-content-type", contentType);
            }
        }
        return generatePresignedUrl(bucketName, objectKey, expiresInSeconds, queryParams);
    }

    public String generateDownloadUrl(String bucketName,
                                      String objectKey,
                                      String fileName,
                                      int expiresInSeconds) {
        ensureObjectExists(bucketName, objectKey);
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("response-content-disposition", buildDisposition("attachment", fileName));
        return generatePresignedUrl(bucketName, objectKey, expiresInSeconds, queryParams);
    }

    public void removeObject(String bucketName, String objectKey) {
        if (!StringUtils.hasText(bucketName) || !StringUtils.hasText(objectKey)) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            throw new BusinessException(500, "删除课件文件失败");
        }
    }

    private String generatePresignedUrl(String bucketName,
                                        String objectKey,
                                        int expiresInSeconds,
                                        Map<String, String> queryParams) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry(expiresInSeconds)
                    .extraQueryParams(queryParams)
                    .build());
        } catch (Exception ex) {
            throw new BusinessException(500, "生成资源访问地址失败");
        }
    }

    private void ensureObjectExists(String bucketName, String objectKey) {
        if (!StringUtils.hasText(bucketName) || !StringUtils.hasText(objectKey)) {
            throw new BusinessException(404, "课程资源文件不存在");
        }
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (ErrorResponseException ex) {
            String code = ex.errorResponse() == null ? null : ex.errorResponse().code();
            if ("NoSuchKey".equals(code) || "NoSuchObject".equals(code) || "NoSuchBucket".equals(code)) {
                throw new BusinessException(404, "课程资源文件不存在");
            }
            throw new BusinessException(500, "校验课程资源文件失败");
        } catch (Exception ex) {
            throw new BusinessException(500, "校验课程资源文件失败");
        }
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String buildAvatarObjectKey(String originalFilename, String userId) {
        String suffix = ".bin";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return "avatars/" + LocalDate.now().format(DATE_PATH_FORMATTER) + "/"
                + userId + "-" + UUID.randomUUID() + suffix;
    }

    private String buildCourseObjectKey(String originalFilename, String teacherId) {
        return "courses/" + teacherId + "/" + LocalDate.now().format(COURSE_PATH_FORMATTER) + "/"
                + UUID.randomUUID() + "-" + sanitizeFilename(originalFilename);
    }

    private String buildObjectUrl(String objectKey) {
        String endpoint = minioProperties.getEndpoint();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + minioProperties.getBucket() + "/" + objectKey;
    }

    private String resolveCourseResourceType(String extension) {
        if (!StringUtils.hasText(extension)) {
            throw new BusinessException(415, "文件类型不支持");
        }
        String normalized = extension.toLowerCase(Locale.ROOT);
        if (VIDEO_EXTENSIONS.contains(normalized)) {
            return "video";
        }
        if (PPT_EXTENSIONS.contains(normalized)) {
            return "ppt";
        }
        if (WORD_EXTENSIONS.contains(normalized)) {
            return "word";
        }
        if (PDF_EXTENSIONS.contains(normalized)) {
            return "pdf";
        }
        return "other";
    }

    private boolean isPreviewInlineType(String resourceType) {
        return "pdf".equals(resourceType) || "video".equals(resourceType);
    }

    private String resolvePreviewContentType(String resourceType, String fileName) {
        if ("pdf".equals(resourceType)) {
            return "application/pdf";
        }
        if ("video".equals(resourceType)) {
            String extension = getExtension(fileName);
            if ("mov".equalsIgnoreCase(extension)) {
                return "video/quicktime";
            }
            if ("webm".equalsIgnoreCase(extension)) {
                return "video/webm";
            }
            return "video/mp4";
        }
        return null;
    }

    private String buildDisposition(String dispositionType, String fileName) {
        String safeFileName = StringUtils.hasText(fileName) ? fileName : "resource";
        String encoded = URLEncoder.encode(safeFileName, StandardCharsets.UTF_8).replace("+", "%20");
        return dispositionType + "; filename*=UTF-8''" + encoded;
    }

    private String getExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return null;
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1);
    }

    private String sanitizeFilename(String originalFilename) {
        String normalized = originalFilename.replace("\\", "/");
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        String safeName = fileName.replaceAll("[^A-Za-z0-9._-]", "-");
        while (safeName.contains("--")) {
            safeName = safeName.replace("--", "-");
        }
        return StringUtils.hasText(safeName) ? safeName : "resource.bin";
    }

    public record UploadResult(String objectKey, String url) {
    }

    public record CourseResourceUploadResult(
            String bucketName,
            String objectKey,
            String url,
            String originalFilename,
            String type,
            long size) {
    }
}
