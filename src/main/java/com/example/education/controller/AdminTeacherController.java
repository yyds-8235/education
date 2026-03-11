package com.example.education.controller;

import com.example.education.common.api.ApiResponse;
import com.example.education.pojo.dto.PersonnelImportResultDTO;
import com.example.education.pojo.dto.TeacherPersonnelSaveRequestDTO;
import com.example.education.pojo.vo.TeacherPersonnelVO;
import com.example.education.service.AdminPersonnelService;
import com.example.education.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/teachers")
public class AdminTeacherController {

    private final AuthService authService;
    private final AdminPersonnelService adminPersonnelService;

    @GetMapping
    public ApiResponse<List<TeacherPersonnelVO>> getTeachers(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "department", required = false) String department) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminPersonnelService.getTeachers(keyword, department));
    }

    @GetMapping("/{teacherId}")
    public ApiResponse<TeacherPersonnelVO> getTeacherDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("teacherId") String teacherId) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminPersonnelService.getTeacherDetail(teacherId));
    }

    @PostMapping
    public ApiResponse<TeacherPersonnelVO> createTeacher(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody TeacherPersonnelSaveRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminPersonnelService.createTeacher(request));
    }

    @PutMapping("/{teacherId}")
    public ApiResponse<TeacherPersonnelVO> updateTeacher(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("teacherId") String teacherId,
            @RequestBody TeacherPersonnelSaveRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminPersonnelService.updateTeacher(teacherId, request));
    }

    @DeleteMapping("/{teacherId}")
    public ApiResponse<Map<String, String>> deleteTeacher(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("teacherId") String teacherId) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminPersonnelService.deleteTeacher(teacherId));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PersonnelImportResultDTO> importTeachers(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestPart("file") MultipartFile file) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminPersonnelService.importTeachers(file));
    }
}
