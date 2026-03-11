package com.example.education.controller;

import com.example.education.common.api.ApiResponse;
import com.example.education.common.util.TimeUtils;
import com.example.education.mapper.UserMapper;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.service.AuthService;
import com.example.education.service.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class FileController {

    private final AuthService authService;
    private final MinioStorageService minioStorageService;
    private final UserMapper userMapper;

    @PostMapping(value = "/upload/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadAvatar(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestPart("file") MultipartFile file) {
        UserEntity user = authService.requireLogin(token);
        MinioStorageService.UploadResult uploadResult = minioStorageService.uploadAvatar(file, user.getId());

        UserEntity update = new UserEntity();
        update.setId(user.getId());
        update.setAvatar(uploadResult.url());
        update.setUpdatedAt(TimeUtils.nowUtc());
        userMapper.updateById(update);

        Map<String, String> data = new LinkedHashMap<>();
        data.put("url", uploadResult.url());
        data.put("avatarUrl", uploadResult.url());
        data.put("avatar", uploadResult.url());
        data.put("objectKey", uploadResult.objectKey());
        return ApiResponse.success(data);
    }

    @PostMapping(value = "/admin/personnel/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, String>> uploadPersonnelAvatar(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestPart("file") MultipartFile file) {
        UserEntity admin = authService.requireAdmin(token);
        MinioStorageService.UploadResult uploadResult = minioStorageService.uploadAvatar(file, admin.getId());

        Map<String, String> data = new LinkedHashMap<>();
        data.put("url", uploadResult.url());
        data.put("avatarUrl", uploadResult.url());
        data.put("objectKey", uploadResult.objectKey());
        return ApiResponse.success(data);
    }
}

