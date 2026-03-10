package com.example.education.controller;

import com.example.education.common.api.ApiResponse;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.pojo.vo.CourseResourceAccessVO;
import com.example.education.service.AuthService;
import com.example.education.service.CourseResourceAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CourseResourceController {

    private final AuthService authService;
    private final CourseResourceAccessService courseResourceAccessService;

    @GetMapping("/course-resources/{resourceId}/preview-url")
    public ApiResponse<CourseResourceAccessVO> getPreviewUrl(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("resourceId") String resourceId) {
        UserEntity user = authService.requireLogin(token);
        return ApiResponse.success(courseResourceAccessService.getPreviewUrl(user, resourceId));
    }

    @GetMapping("/course-resources/{resourceId}/download-url")
    public ApiResponse<CourseResourceAccessVO> getDownloadUrl(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("resourceId") String resourceId) {
        UserEntity user = authService.requireLogin(token);
        return ApiResponse.success(courseResourceAccessService.getDownloadUrl(user, resourceId));
    }
}
