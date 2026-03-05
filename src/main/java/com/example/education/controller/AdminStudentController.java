package com.example.education.controller;

import com.example.education.common.api.ApiResponse;
import com.example.education.pojo.dto.*;
import com.example.education.pojo.query.StudentQuery;
import com.example.education.pojo.vo.StudentProfileVO;
import com.example.education.service.AdminStudentService;
import com.example.education.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/students")
public class AdminStudentController {

    private final AuthService authService;
    private final AdminStudentService adminStudentService;

    @GetMapping
    public ApiResponse<StudentPageDataDTO> getStudents(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "page", required = false) Long page,
            @RequestParam(value = "pageSize", required = false) Long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "class", required = false) String className,
            @RequestParam(value = "archiveFilter", required = false) String archiveFilter,
            @RequestParam(value = "canView", required = false) Boolean canView,
            @RequestParam(value = "canEdit", required = false) Boolean canEdit) {
        authService.requireAdmin(token);
        StudentQuery query = new StudentQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setKeyword(keyword);
        query.setGrade(grade);
        query.setClassName(className);
        query.setArchiveFilter(archiveFilter);
        query.setCanView(canView);
        query.setCanEdit(canEdit);
        return ApiResponse.success(adminStudentService.getStudents(query));
    }

    @GetMapping("/{studentId}")
    public ApiResponse<StudentProfileVO> getStudentDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("studentId") String studentId) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.getStudentDetail(studentId));
    }

    @PostMapping
    public ApiResponse<StudentProfileVO> createStudent(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody CreateStudentRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.createStudent(request));
    }

    @PutMapping("/{studentId}")
    public ApiResponse<StudentProfileVO> updateStudent(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("studentId") String studentId,
            @Valid @RequestBody UpdateStudentRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.updateStudent(studentId, request));
    }

    @DeleteMapping("/{studentId}")
    public ApiResponse<Map<String, String>> deleteStudent(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("studentId") String studentId) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.deleteStudent(studentId));
    }

    @PatchMapping("/{studentId}/permissions")
    public ApiResponse<StudentPermissionResponseDTO> updatePermissions(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("studentId") String studentId,
            @Valid @RequestBody UpdateStudentPermissionRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.updatePermissions(studentId, request));
    }

    @PostMapping("/sync")
    public ApiResponse<SyncStudentsResponseDTO> syncStudents(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody(required = false) SyncStudentsRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.syncStudents(request));
    }

    @GetMapping("/meta")
    public ApiResponse<StudentMetaResponseDTO> getMeta(
            @RequestHeader(value = "Authorization", required = false) String token) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.getMeta());
    }
}

