package com.example.education.service;

import com.example.education.common.exception.BusinessException;
import com.example.education.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private static final DateTimeFormatter DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

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

    private String buildObjectUrl(String objectKey) {
        String endpoint = minioProperties.getEndpoint();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + minioProperties.getBucket() + "/" + objectKey;
    }

    public record UploadResult(String objectKey, String url) {
    }
}

