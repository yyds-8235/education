package com.example.education.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.education.common.exception.BusinessException;
import com.example.education.common.util.TimeUtils;
import com.example.education.mapper.StudentProfileMapper;
import com.example.education.mapper.UserMapper;
import com.example.education.pojo.dto.*;
import com.example.education.pojo.entity.StudentProfileEntity;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.pojo.model.StudentImportRow;
import com.example.education.pojo.model.StudentProfileRow;
import com.example.education.pojo.query.StudentQuery;
import com.example.education.pojo.vo.StudentProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStudentService {

    private static final String ROLE_STUDENT = "student";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final Set<String> ALLOWED_STATUS = Set.of("active", "inactive", "suspended");
    private static final Set<String> EXCEL_EXTENSIONS = Set.of("xls", "xlsx");

    private static final List<String> META_GRADES = List.of("初一", "初二", "初三", "高一", "高二", "高三");
    private static final List<String> META_CLASSES = List.of("1班", "2班", "3班", "4班");
    private static final List<String> META_POVERTY_LEVELS = List.of("非困难", "一般困难", "困难", "特别困难");
    private static final List<String> META_HOUSEHOLD_TYPES = List.of("城镇", "农村");

    private final StudentProfileMapper studentProfileMapper;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public StudentPageDataDTO getStudents(StudentQuery query) {
        long page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : query.getPageSize();
        query.setPage(page);
        query.setPageSize(pageSize);

        Page<StudentProfileRow> pageQuery = Page.of(page, pageSize);
        IPage<StudentProfileRow> pageResult = studentProfileMapper.selectStudentPage(pageQuery, query);

        StudentPageDataDTO data = new StudentPageDataDTO();
        data.setList(pageResult.getRecords().stream().map(this::toView).toList());
        data.setTotal(pageResult.getTotal());
        data.setPage(page);
        data.setPageSize(pageSize);
        data.setTotalPages(pageResult.getPages());
        return data;
    }

    public StudentProfileVO getStudentDetail(String studentId) {
        StudentProfileRow row = studentProfileMapper.selectStudentDetail(studentId);
        if (row == null) {
            throw new BusinessException(404, "学生不存在");
        }
        return toView(row);
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentProfileVO createStudent(CreateStudentRequestDTO request) {
        validateStudentUsername(request.getUsername());
        ensureUsernameUnique(request.getUsername(), null);
        ensureStudentNoUnique(request.getStudentNo(), null);

        String studentId = UUID.randomUUID().toString();
        LocalDateTime now = TimeUtils.nowUtc();

        UserEntity user = new UserEntity();
        user.setId(studentId);
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole("student");
        user.setStatus("active");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setAvatar(request.getAvatar());
        userMapper.insert(user);

        StudentProfileEntity studentProfile = new StudentProfileEntity();
        studentProfile.setStudentId(studentId);
        studentProfile.setStudentNo(request.getStudentNo());
        studentProfile.setGrade(request.getGrade());
        studentProfile.setClassName(request.getClazz());
        studentProfile.setGuardian(request.getGuardian());
        studentProfile.setPovertyLevel(request.getPovertyLevel());
        studentProfile.setIsSponsored(request.getIsSponsored());
        studentProfile.setHouseholdType(request.getHouseholdType());
        studentProfile.setIsLeftBehind(request.getIsLeftBehind());
        studentProfile.setIsDisabled(request.getIsDisabled());
        studentProfile.setIsSingleParent(request.getIsSingleParent());
        studentProfile.setIsKeyConcern(request.getIsKeyConcern());
        studentProfile.setCanView(request.getCanView());
        studentProfile.setCanEdit(Boolean.TRUE.equals(request.getCanView()) ? request.getCanEdit() : false);
        studentProfileMapper.insert(studentProfile);

        return getStudentDetail(studentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentProfileVO updateStudent(String studentId, UpdateStudentRequestDTO request) {
        UserEntity existingUser = getStudentUser(studentId);
        StudentProfileEntity existingProfile = studentProfileMapper.selectById(studentId);
        if (existingProfile == null) {
            throw new BusinessException(404, "学生不存在");
        }

        validateStudentUsername(request.getUsername());
        ensureUsernameUnique(request.getUsername(), studentId);
        ensureStudentNoUnique(request.getStudentNo(), studentId);

        LocalDateTime now = TimeUtils.nowUtc();

        existingUser.setUsername(request.getUsername());
        existingUser.setRealName(request.getName());
        existingUser.setEmail(request.getEmail());
        existingUser.setPhone(request.getPhone());
        existingUser.setUpdatedAt(now);
        if (StringUtils.hasText(request.getPassword())) {
            existingUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        existingUser.setAvatar(request.getAvatar());
        userMapper.updateById(existingUser);

        existingProfile.setStudentNo(request.getStudentNo());
        existingProfile.setGrade(request.getGrade());
        existingProfile.setClassName(request.getClazz());
        existingProfile.setGuardian(request.getGuardian());
        existingProfile.setPovertyLevel(request.getPovertyLevel());
        existingProfile.setIsSponsored(request.getIsSponsored());
        existingProfile.setHouseholdType(request.getHouseholdType());
        existingProfile.setIsLeftBehind(request.getIsLeftBehind());
        existingProfile.setIsDisabled(request.getIsDisabled());
        existingProfile.setIsSingleParent(request.getIsSingleParent());
        existingProfile.setIsKeyConcern(request.getIsKeyConcern());
        existingProfile.setCanView(request.getCanView());
        existingProfile.setCanEdit(Boolean.TRUE.equals(request.getCanView()) ? request.getCanEdit() : false);
        studentProfileMapper.updateById(existingProfile);

        return getStudentDetail(studentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> deleteStudent(String studentId) {
        // 直接删除
        userMapper.deleteById(studentId);
        studentProfileMapper.deleteById(studentId);
        return Collections.singletonMap("id", studentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentImportResultDTO importStudents(MultipartFile file) {
        validateExcelFile(file);
        List<StudentImportRow> rows = readExcel(file, StudentImportRow.class);

        StudentImportResultDTO result = new StudentImportResultDTO();
        result.setTotal(rows.size());

        int rowNumber = 2;
        for (StudentImportRow row : rows) {
            if (isStudentRowEmpty(row)) {
                addImportError(result, rowNumber, null, "empty row");
                rowNumber++;
                continue;
            }

            try {
                importStudentRow(row);
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception ex) {
                addImportError(result, rowNumber, row.getStudentNo(), resolveImportReason(ex));
            }
            rowNumber++;
        }

        result.setFailed(result.getErrors().size());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentPermissionResponseDTO updatePermissions(String studentId, UpdateStudentPermissionRequestDTO request) {
        getStudentUser(studentId);
        StudentProfileEntity profile = studentProfileMapper.selectById(studentId);
        if (profile == null) {
            throw new BusinessException(404, "学生不存在");
        }

        boolean canView = Boolean.TRUE.equals(request.getCanView());
        boolean canEdit = canView && Boolean.TRUE.equals(request.getCanEdit());

        profile.setCanView(canView);
        profile.setCanEdit(canEdit);
        studentProfileMapper.updateById(profile);

        LocalDateTime now = TimeUtils.nowUtc();
        UserEntity user = new UserEntity();
        user.setId(studentId);
        user.setUpdatedAt(now);
        userMapper.updateById(user);

        StudentPermissionResponseDTO response = new StudentPermissionResponseDTO();
        response.setStudentId(studentId);
        response.setCanView(canView);
        response.setCanEdit(canEdit);
        response.setUpdatedAt(TimeUtils.toIsoUtc(now));
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public SyncStudentsResponseDTO syncStudents(SyncStudentsRequestDTO request) {
        List<String> requestIds = request == null ? null : request.getStudentIds();
        List<String> existingStudentIds = studentProfileMapper.selectExistingStudentIds(requestIds);
        if (CollectionUtils.isEmpty(existingStudentIds)) {
            SyncStudentsResponseDTO response = new SyncStudentsResponseDTO();
            response.setSyncedCount(0);
            response.setSyncedAt(TimeUtils.toDateTime(TimeUtils.nowUtc()));
            response.setFailed(requestIds == null ? Collections.emptyList() : requestIds);
            return response;
        }

        LocalDateTime now = TimeUtils.nowUtc();
//        studentProfileMapper.update(
//                null,
//                new LambdaUpdateWrapper<StudentProfileEntity>()
//                        .in(StudentProfileEntity::getStudentId, existingStudentIds)
//                        .set(StudentProfileEntity::getSyncedAt, now));

        SyncStudentsResponseDTO response = new SyncStudentsResponseDTO();
        response.setSyncedCount(existingStudentIds.size());
        response.setSyncedAt(TimeUtils.toDateTime(now));
        response.setFailed(computeFailedIds(requestIds, existingStudentIds));
        return response;
    }

    public StudentMetaResponseDTO getMeta() {
        StudentMetaResponseDTO response = new StudentMetaResponseDTO();
        response.setGrades(META_GRADES);
        response.setClasses(META_CLASSES);
        response.setPovertyLevels(META_POVERTY_LEVELS);
        response.setHouseholdTypes(META_HOUSEHOLD_TYPES);
        return response;
    }

    private void validateStudentUsername(String username) {
        if (!StringUtils.hasText(username) || !username.trim().startsWith("stu")) {
            throw new BusinessException(422, "用户名前缀非法");
        }
    }

    private void ensureUsernameUnique(String username, String selfId) {
        UserEntity existing = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username)
                .last("LIMIT 1"));
        if (existing == null) {
            return;
        }
        if (selfId == null || !selfId.equals(existing.getId())) {
            throw new BusinessException(409, "用户名已存在");
        }
    }

    private void ensureStudentNoUnique(String studentNo, String selfId) {
        StudentProfileEntity existing = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfileEntity>()
                .eq(StudentProfileEntity::getStudentNo, studentNo)
                .last("LIMIT 1"));
        if (existing == null) {
            return;
        }
        if (selfId == null || !selfId.equals(existing.getStudentId())) {
            throw new BusinessException(409, "学号已存在");
        }
    }

    private UserEntity getStudentUser(String studentId) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getId, studentId)
                .eq(UserEntity::getRole, "student")
                .ne(UserEntity::getStatus, "inactive")
                .last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException(404, "学生不存在");
        }
        return user;
    }

    private void importStudentRow(StudentImportRow row) {
        validateStudentImportRow(row);

        String username = normalize(row.getUsername());
        String studentNo = normalize(row.getStudentNo());

        ensureUsernameUnique(username, null);
        ensureStudentNoUnique(studentNo, null);

        String studentId = UUID.randomUUID().toString();
        LocalDateTime now = TimeUtils.nowUtc();

        UserEntity user = new UserEntity();
        user.setId(studentId);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(resolvePassword(row.getPassword())));
        user.setRealName(normalize(row.getRealName()));
        user.setEmail(normalizeNullable(row.getEmail()));
        user.setPhone(normalizeNullable(row.getPhone()));
        user.setAvatar(normalizeNullable(row.getAvatar()));
        user.setRole(ROLE_STUDENT);
        user.setStatus(normalizeStatus(row.getStatus()));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        StudentProfileEntity profile = new StudentProfileEntity();
        profile.setStudentId(studentId);
        profile.setStudentNo(studentNo);
        profile.setGrade(normalize(row.getGrade()));
        profile.setClassName(normalize(row.getClassName()));
        profile.setGuardian(normalizeNullable(row.getGuardian()));
        profile.setPovertyLevel(normalizeNullable(row.getPovertyLevel()));
        profile.setIsSponsored(defaultFalse(row.getIsSponsored()));
        profile.setHouseholdType(normalizeNullable(row.getHouseholdType()));
        profile.setIsLeftBehind(defaultFalse(row.getIsLeftBehind()));
        profile.setIsDisabled(defaultFalse(row.getIsDisabled()));
        profile.setIsSingleParent(defaultFalse(row.getIsSingleParent()));
        profile.setIsKeyConcern(defaultFalse(row.getIsKeyConcern()));
        boolean canView = defaultFalse(row.getCanView());
        profile.setCanView(canView);
        profile.setCanEdit(canView && defaultFalse(row.getCanEdit()));
        try {
            userMapper.insert(user);
            studentProfileMapper.insert(profile);
        } catch (Exception ex) {
            studentProfileMapper.deleteById(studentId);
            userMapper.deleteById(studentId);
            throw ex;
        }
    }

    private void validateStudentImportRow(StudentImportRow row) {
        if (row == null) {
            throw new BusinessException(422, "row is required");
        }
        validateStudentUsername(row.getUsername());
        if (!StringUtils.hasText(row.getRealName())) {
            throw new BusinessException(422, "realName is required");
        }
        if (!StringUtils.hasText(row.getStatus())) {
            throw new BusinessException(422, "status is required");
        }
        if (!StringUtils.hasText(row.getStudentNo())) {
            throw new BusinessException(422, "studentNo is required");
        }
        if (!StringUtils.hasText(row.getGrade())) {
            throw new BusinessException(422, "grade is required");
        }
        if (!StringUtils.hasText(row.getClassName())) {
            throw new BusinessException(422, "className is required");
        }
        normalizeStatus(row.getStatus());
    }

    private void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(422, "file is required");
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new BusinessException(422, "invalid excel file");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!EXCEL_EXTENSIONS.contains(extension)) {
            throw new BusinessException(422, "only .xls or .xlsx is supported");
        }
    }

    private <T> List<T> readExcel(MultipartFile file, Class<T> headClass) {
        List<T> rows = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            EasyExcel.read(inputStream, headClass, new AnalysisEventListener<T>() {
                @Override
                public void invoke(T data, AnalysisContext context) {
                    rows.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).autoCloseStream(false).sheet().doRead();
            return rows;
        } catch (IOException ex) {
            throw new BusinessException(400, "failed to read upload file");
        } catch (Exception ex) {
            throw new BusinessException(400, "failed to parse excel");
        }
    }

    private boolean isStudentRowEmpty(StudentImportRow row) {
        return row == null || isBlank(row.getUsername())
                && isBlank(row.getPassword())
                && isBlank(row.getRealName())
                && isBlank(row.getEmail())
                && isBlank(row.getPhone())
                && isBlank(row.getAvatar())
                && isBlank(row.getStatus())
                && isBlank(row.getStudentNo())
                && isBlank(row.getGrade())
                && isBlank(row.getClassName())
                && isBlank(row.getGuardian())
                && isBlank(row.getPovertyLevel())
                && isBlank(row.getHouseholdType())
                && row.getIsSponsored() == null
                && row.getIsLeftBehind() == null
                && row.getIsDisabled() == null
                && row.getIsSingleParent() == null
                && row.getIsKeyConcern() == null
                && row.getCanView() == null
                && row.getCanEdit() == null;
    }

    private void addImportError(StudentImportResultDTO result, int rowNumber, String studentNo, String error) {
        StudentImportErrorDTO item = new StudentImportErrorDTO();
        item.setRow(rowNumber);
        item.setStudentNo(studentNo);
        item.setError(error);
        result.getErrors().add(item);
    }

    private String resolveImportReason(Exception ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof BusinessException businessException) {
                return businessException.getMessage();
            }
            current = current.getCause();
        }
        return "import failed";
    }

    private String resolvePassword(String password) {
        return StringUtils.hasText(password) ? password.trim() : DEFAULT_PASSWORD;
    }

    private String normalizeStatus(String status) {
        String normalized = normalize(status);
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new BusinessException(422, "invalid status");
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return StringUtils.hasText(normalized) ? normalized : null;
    }

    private boolean defaultFalse(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private boolean isBlank(String value) {
        return !StringUtils.hasText(value);
    }

    private StudentProfileVO toView(StudentProfileRow row) {
        StudentProfileVO view = new StudentProfileVO();
        view.setId(row.getId());
        view.setAvatar(row.getAvatar());
        view.setStudentNo(row.getStudentNo());
        view.setName(row.getName());
        view.setUsername(row.getUsername());
        view.setGrade(row.getGrade());
        view.setClazz(row.getClassName());
        view.setGuardian(row.getGuardian());
        view.setSyncedAt(TimeUtils.toDateTime(row.getSyncedAt()));
        view.setPovertyLevel(row.getPovertyLevel());
        view.setIsSponsored(row.getIsSponsored());
        view.setHouseholdType(row.getHouseholdType());
        view.setIsLeftBehind(row.getIsLeftBehind());
        view.setIsDisabled(row.getIsDisabled());
        view.setIsSingleParent(row.getIsSingleParent());
        view.setIsKeyConcern(row.getIsKeyConcern());
        view.setCanView(row.getCanView());
        view.setCanEdit(row.getCanEdit());
        view.setEmail(row.getEmail());
        view.setPhone(row.getPhone());
        return view;
    }

    private List<String> computeFailedIds(List<String> requestIds, List<String> existingStudentIds) {
        if (CollectionUtils.isEmpty(requestIds)) {
            return Collections.emptyList();
        }
        Map<String, Boolean> existingMap = existingStudentIds.stream()
                .collect(Collectors.toMap(Function.identity(), item -> true));
        List<String> failed = new ArrayList<>();
        for (String requestId : requestIds) {
            if (!existingMap.containsKey(requestId)) {
                failed.add(requestId);
            }
        }
        return failed;
    }
}

