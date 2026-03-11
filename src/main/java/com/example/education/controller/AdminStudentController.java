package com.example.education.controller;

import com.example.education.common.api.ApiResponse;
import com.example.education.pojo.dto.*;
import com.example.education.pojo.query.StudentQuery;
import com.example.education.pojo.vo.StudentProfileVO;
import com.example.education.service.AdminStudentService;
import com.example.education.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @RequestParam(value = "page", required = true) Long page,
            @RequestParam(value = "pageSize", required = true) Long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "archiveFilter", required = false) String archiveFilter) {
        authService.requireAdmin(token);
        StudentQuery query = new StudentQuery();
        query.setPage(page);
        query.setPageSize(pageSize);
        query.setKeyword(keyword);
        query.setGrade(grade);
        query.setArchiveFilter(archiveFilter);
        return ApiResponse.success(adminStudentService.getStudents(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudentProfileVO> getStudentDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("id") String id) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.getStudentDetail(id));
    }

    @PostMapping
    public ApiResponse<StudentProfileVO> createStudent(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody CreateStudentRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.createStudent(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudentProfileVO> updateStudent(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateStudentRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.updateStudent(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, String>> deleteStudent(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("id") String id) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.deleteStudent(id));
    }

//    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ApiResponse<StudentImportResultDTO> importStudents(
//            @RequestHeader(value = "Authorization", required = false) String token,
//            @RequestPart("file") MultipartFile file) {
//        authService.requireAdmin(token);
//        return ApiResponse.success(adminStudentService.importStudents(file));
//    }

    @PatchMapping("/{id}/permissions")
    public ApiResponse<StudentPermissionResponseDTO> updatePermissions(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("id") String id,
            @Valid @RequestBody UpdateStudentPermissionRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.updatePermissions(id, request));
    }

//    @PostMapping("/{id}/reset-password")
//    public ApiResponse<Void> resetPassword(
//            @RequestHeader(value = "Authorization", required = false) String token,
//            @PathVariable("id") String id,
//            @Valid @RequestBody ResetPasswordRequestDTO request) {
//        authService.requireAdmin(token);
//        adminStudentService.resetPassword(id, request);
//        return ApiResponse.success(null);
//    }

}

