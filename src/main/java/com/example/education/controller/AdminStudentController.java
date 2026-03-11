package com.example.education.controller;

import com.example.education.common.api.ApiResponse;
import com.example.education.pojo.dto.*;
import com.example.education.pojo.query.StudentQuery;
import com.example.education.pojo.vo.StudentPersonnelVO;
import com.example.education.pojo.vo.StudentProfileVO;
import com.example.education.service.AdminPersonnelService;
import com.example.education.service.AdminStudentService;
import com.example.education.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/students")
public class AdminStudentController {

    private final AuthService authService;
    private final AdminPersonnelService adminPersonnelService;
    private final AdminStudentService adminStudentService;

    @GetMapping("/xq")
    public ApiResponse<StudentPageDataDTO> getStudents2(
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

    @GetMapping("/xq/{studentId}")
    public ApiResponse<StudentProfileVO> getStudentDetail2(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("studentId") String studentId) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.getStudentDetail(studentId));
    }

    @PostMapping("/xq")
    public ApiResponse<StudentProfileVO> createStudent2(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody CreateStudentRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.createStudent(request));
    }

    @PutMapping("/xq/{studentId}")
    public ApiResponse<StudentProfileVO> updateStudent2(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("studentId") String studentId,
            @Valid @RequestBody UpdateStudentRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminStudentService.updateStudent(studentId, request));
    }

    @DeleteMapping("/xq/{studentId}")
    public ApiResponse<Map<String, String>> deleteStudent2(
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


    @GetMapping
    public ApiResponse<List<StudentPersonnelVO>> getStudents(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "class", required = false) String className) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminPersonnelService.getStudents(keyword, grade, className));
    }

    @GetMapping("/{studentId}")
    public ApiResponse<StudentPersonnelVO> getStudentDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("studentId") String studentId) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminPersonnelService.getStudentDetail(studentId));
    }

    @PostMapping
    public ApiResponse<StudentPersonnelVO> createStudent(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody StudentPersonnelSaveRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminPersonnelService.createStudent(request));
    }

    @PutMapping("/{studentId}")
    public ApiResponse<StudentPersonnelVO> updateStudent(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("studentId") String studentId,
            @RequestBody StudentPersonnelSaveRequestDTO request) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminPersonnelService.updateStudent(studentId, request));
    }

    @DeleteMapping("/{studentId}")
    public ApiResponse<Map<String, String>> deleteStudent(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable("studentId") String studentId) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminPersonnelService.deleteStudent(studentId));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PersonnelImportResultDTO> importStudents(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestPart("file") MultipartFile file) {
        authService.requireAdmin(token);
        return ApiResponse.success(adminPersonnelService.importStudents(file));
    }

}

