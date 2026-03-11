package com.example.education.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.education.common.exception.BusinessException;
import com.example.education.common.util.TimeUtils;
import com.example.education.mapper.StudentProfileMapper;
import com.example.education.mapper.TeacherProfileMapper;
import com.example.education.mapper.UserMapper;
import com.example.education.pojo.dto.PersonnelImportFailureDTO;
import com.example.education.pojo.dto.PersonnelImportResultDTO;
import com.example.education.pojo.dto.StudentPersonnelSaveRequestDTO;
import com.example.education.pojo.dto.TeacherPersonnelSaveRequestDTO;
import com.example.education.pojo.entity.StudentProfileEntity;
import com.example.education.pojo.entity.TeacherProfileEntity;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.pojo.model.StudentImportRow;
import com.example.education.pojo.model.TeacherImportRow;
import com.example.education.pojo.vo.StudentPersonnelVO;
import com.example.education.pojo.vo.TeacherPersonnelVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPersonnelService {

    private static final String ROLE_STUDENT = "student";
    private static final String ROLE_TEACHER = "teacher";
    private static final String DEFAULT_PASSWORD = "123456";
    private static final Set<String> ALLOWED_STATUS = Set.of("active", "inactive", "suspended");
    private static final Set<String> EXCEL_EXTENSIONS = Set.of("xls", "xlsx");

    private final UserMapper userMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public List<StudentPersonnelVO> getStudents(String keyword, String grade, String className) {
        List<UserEntity> users = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getRole, ROLE_STUDENT)
                .orderByDesc(UserEntity::getCreatedAt));
        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, StudentProfileEntity> profileMap = studentProfileMapper.selectBatchIds(extractUserIds(users)).stream()
                .collect(Collectors.toMap(StudentProfileEntity::getStudentId, profile -> profile));

        return users.stream()
                .map(user -> toStudentView(user, profileMap.get(user.getId())))
                .filter(Objects::nonNull)
                .filter(view -> matchesStudent(view, keyword, grade, className))
                .sorted(Comparator.comparing(StudentPersonnelVO::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public StudentPersonnelVO getStudentDetail(String studentId) {
        UserEntity user = getUser(studentId, ROLE_STUDENT);
        StudentProfileEntity profile = studentProfileMapper.selectById(studentId);
        if (profile == null) {
            throw new BusinessException(404, "student not found");
        }
        return toStudentView(user, profile);
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentPersonnelVO createStudent(StudentPersonnelSaveRequestDTO request) {
        validateStudentRequest(request);
        ensureUsernameUnique(request.getUsername(), null);
        ensureStudentNoUnique(request.getStudentNo(), null);

        String studentId = UUID.randomUUID().toString();
        LocalDateTime now = TimeUtils.nowUtc();

        UserEntity user = new UserEntity();
        user.setId(studentId);
        user.setUsername(normalize(request.getUsername()));
        user.setPasswordHash(passwordEncoder.encode(resolvePassword(request.getPassword())));
        user.setRealName(normalize(request.getRealName()));
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setPhone(normalizeNullable(request.getPhone()));
        user.setAvatar(normalizeNullable(request.getAvatar()));
        user.setRole(ROLE_STUDENT);
        user.setStatus(normalizeStatus(request.getStatus()));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        StudentProfileEntity profile = new StudentProfileEntity();
        profile.setStudentId(studentId);
        profile.setStudentNo(normalize(request.getStudentNo()));
        profile.setGrade(normalize(request.getGrade()));
        profile.setClassName(normalize(request.getClassName()));
        profile.setGuardian(normalizeNullable(request.getGuardian()));
        profile.setIsSponsored(Boolean.FALSE);
        profile.setIsLeftBehind(Boolean.FALSE);
        profile.setIsDisabled(Boolean.FALSE);
        profile.setIsSingleParent(Boolean.FALSE);
        profile.setIsKeyConcern(Boolean.FALSE);
        profile.setCanView(Boolean.FALSE);
        profile.setCanEdit(Boolean.FALSE);
        studentProfileMapper.insert(profile);

        return getStudentDetail(studentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public StudentPersonnelVO updateStudent(String studentId, StudentPersonnelSaveRequestDTO request) {
        validateStudentRequest(request);

        UserEntity user = getUser(studentId, ROLE_STUDENT);
        StudentProfileEntity profile = studentProfileMapper.selectById(studentId);
        if (profile == null) {
            throw new BusinessException(404, "student not found");
        }

        ensureUsernameUnique(request.getUsername(), studentId);
        ensureStudentNoUnique(request.getStudentNo(), studentId);

        user.setUsername(normalize(request.getUsername()));
        user.setRealName(normalize(request.getRealName()));
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setPhone(normalizeNullable(request.getPhone()));
        user.setAvatar(normalizeNullable(request.getAvatar()));
        user.setStatus(normalizeStatus(request.getStatus()));
        user.setUpdatedAt(TimeUtils.nowUtc());
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        }
        userMapper.updateById(user);

        profile.setStudentNo(normalize(request.getStudentNo()));
        profile.setGrade(normalize(request.getGrade()));
        profile.setClassName(normalize(request.getClassName()));
        profile.setGuardian(normalizeNullable(request.getGuardian()));
        studentProfileMapper.updateById(profile);

        return getStudentDetail(studentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> deleteStudent(String studentId) {
        getStudentDetail(studentId);
        studentProfileMapper.deleteById(studentId);
        userMapper.deleteById(studentId);
        return Collections.singletonMap("id", studentId);
    }

    public PersonnelImportResultDTO importStudents(MultipartFile file) {
        validateExcelFile(file);
        List<StudentImportRow> rows = readExcel(file, StudentImportRow.class);
        PersonnelImportResultDTO result = new PersonnelImportResultDTO();
        int rowNumber = 2;
        for (StudentImportRow row : rows) {
            if (isStudentRowEmpty(row)) {
                addFailedRow(result, rowNumber, "empty row");
                rowNumber++;
                continue;
            }

            StudentPersonnelSaveRequestDTO request = new StudentPersonnelSaveRequestDTO();
            request.setUsername(row.getUsername());
            request.setRealName(row.getRealName());
            request.setEmail(row.getEmail());
            request.setPhone(row.getPhone());
            request.setAvatar(row.getAvatar());
            request.setStatus(row.getStatus());
            request.setStudentNo(row.getStudentNo());
            request.setGrade(row.getGrade());
            request.setClassName(row.getClassName());
            request.setGuardian(row.getGuardian());
            try {
                createStudent(request);
                result.setImportedCount(result.getImportedCount() + 1);
            } catch (Exception ex) {
                addFailedRow(result, rowNumber, resolveImportReason(ex));
            }
            rowNumber++;
        }
        return result;
    }

    public List<TeacherPersonnelVO> getTeachers(String keyword, String department) {
        List<UserEntity> users = userMapper.selectList(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getRole, ROLE_TEACHER)
                .orderByDesc(UserEntity::getCreatedAt));
        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, TeacherProfileEntity> profileMap = teacherProfileMapper.selectBatchIds(extractUserIds(users)).stream()
                .collect(Collectors.toMap(TeacherProfileEntity::getTeacherId, profile -> profile));

        return users.stream()
                .map(user -> toTeacherView(user, profileMap.get(user.getId())))
                .filter(Objects::nonNull)
                .filter(view -> matchesTeacher(view, keyword, department))
                .sorted(Comparator.comparing(TeacherPersonnelVO::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public TeacherPersonnelVO getTeacherDetail(String teacherId) {
        UserEntity user = getUser(teacherId, ROLE_TEACHER);
        TeacherProfileEntity profile = teacherProfileMapper.selectById(teacherId);
        if (profile == null) {
            throw new BusinessException(404, "teacher not found");
        }
        return toTeacherView(user, profile);
    }

    @Transactional(rollbackFor = Exception.class)
    public TeacherPersonnelVO createTeacher(TeacherPersonnelSaveRequestDTO request) {
        validateTeacherRequest(request);
        ensureUsernameUnique(request.getUsername(), null);
        ensureTeacherNoUnique(request.getTeacherNo(), null);

        String teacherId = UUID.randomUUID().toString();
        LocalDateTime now = TimeUtils.nowUtc();

        UserEntity user = new UserEntity();
        user.setId(teacherId);
        user.setUsername(normalize(request.getUsername()));
        user.setPasswordHash(passwordEncoder.encode(resolvePassword(request.getPassword())));
        user.setRealName(normalize(request.getRealName()));
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setPhone(normalizeNullable(request.getPhone()));
        user.setAvatar(normalizeNullable(request.getAvatar()));
        user.setRole(ROLE_TEACHER);
        user.setStatus(normalizeStatus(request.getStatus()));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userMapper.insert(user);

        TeacherProfileEntity profile = new TeacherProfileEntity();
        profile.setTeacherId(teacherId);
        profile.setTeacherNo(normalize(request.getTeacherNo()));
        profile.setDepartment(normalize(request.getDepartment()));
        profile.setSubjectsJson(writeSubjects(request.getSubjects()));
        teacherProfileMapper.insert(profile);

        return getTeacherDetail(teacherId);
    }

    @Transactional(rollbackFor = Exception.class)
    public TeacherPersonnelVO updateTeacher(String teacherId, TeacherPersonnelSaveRequestDTO request) {
        validateTeacherRequest(request);

        UserEntity user = getUser(teacherId, ROLE_TEACHER);
        TeacherProfileEntity profile = teacherProfileMapper.selectById(teacherId);
        if (profile == null) {
            throw new BusinessException(404, "teacher not found");
        }

        ensureUsernameUnique(request.getUsername(), teacherId);
        ensureTeacherNoUnique(request.getTeacherNo(), teacherId);

        user.setUsername(normalize(request.getUsername()));
        user.setRealName(normalize(request.getRealName()));
        user.setEmail(normalizeNullable(request.getEmail()));
        user.setPhone(normalizeNullable(request.getPhone()));
        user.setAvatar(normalizeNullable(request.getAvatar()));
        user.setStatus(normalizeStatus(request.getStatus()));
        user.setUpdatedAt(TimeUtils.nowUtc());
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword().trim()));
        }
        userMapper.updateById(user);

        profile.setTeacherNo(normalize(request.getTeacherNo()));
        profile.setDepartment(normalize(request.getDepartment()));
        profile.setSubjectsJson(writeSubjects(request.getSubjects()));
        teacherProfileMapper.updateById(profile);

        return getTeacherDetail(teacherId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, String> deleteTeacher(String teacherId) {
        getTeacherDetail(teacherId);
        teacherProfileMapper.deleteById(teacherId);
        userMapper.deleteById(teacherId);
        return Collections.singletonMap("id", teacherId);
    }

    public PersonnelImportResultDTO importTeachers(MultipartFile file) {
        validateExcelFile(file);
        List<TeacherImportRow> rows = readExcel(file, TeacherImportRow.class);
        PersonnelImportResultDTO result = new PersonnelImportResultDTO();
        int rowNumber = 2;
        for (TeacherImportRow row : rows) {
            if (isTeacherRowEmpty(row)) {
                addFailedRow(result, rowNumber, "empty row");
                rowNumber++;
                continue;
            }

            TeacherPersonnelSaveRequestDTO request = new TeacherPersonnelSaveRequestDTO();
            request.setUsername(row.getUsername());
            request.setRealName(row.getRealName());
            request.setEmail(row.getEmail());
            request.setPhone(row.getPhone());
            request.setAvatar(row.getAvatar());
            request.setStatus(row.getStatus());
            request.setTeacherNo(row.getTeacherNo());
            request.setDepartment(row.getDepartment());
            request.setSubjects(splitSubjects(row.getSubjects()));
            try {
                createTeacher(request);
                result.setImportedCount(result.getImportedCount() + 1);
            } catch (Exception ex) {
                addFailedRow(result, rowNumber, resolveImportReason(ex));
            }
            rowNumber++;
        }
        return result;
    }

    private StudentPersonnelVO toStudentView(UserEntity user, StudentProfileEntity profile) {
        if (user == null || profile == null) {
            return null;
        }
        StudentPersonnelVO view = new StudentPersonnelVO();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setRealName(user.getRealName());
        view.setEmail(user.getEmail());
        view.setPhone(user.getPhone());
        view.setAvatar(user.getAvatar());
        view.setStatus(user.getStatus());
        view.setCreatedAt(TimeUtils.toDateTime(user.getCreatedAt()));
        view.setStudentNo(profile.getStudentNo());
        view.setGrade(profile.getGrade());
        view.setClassName(profile.getClassName());
        view.setGuardian(profile.getGuardian());
        return view;
    }

    private TeacherPersonnelVO toTeacherView(UserEntity user, TeacherProfileEntity profile) {
        if (user == null || profile == null) {
            return null;
        }
        TeacherPersonnelVO view = new TeacherPersonnelVO();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setRealName(user.getRealName());
        view.setEmail(user.getEmail());
        view.setPhone(user.getPhone());
        view.setAvatar(user.getAvatar());
        view.setStatus(user.getStatus());
        view.setCreatedAt(TimeUtils.toDateTime(user.getCreatedAt()));
        view.setTeacherNo(profile.getTeacherNo());
        view.setDepartment(profile.getDepartment());
        view.setSubjects(readSubjects(profile.getSubjectsJson()));
        return view;
    }

    private boolean matchesStudent(StudentPersonnelVO view, String keyword, String grade, String className) {
        if (StringUtils.hasText(grade) && !Objects.equals(grade.trim(), view.getGrade())) {
            return false;
        }
        if (StringUtils.hasText(className) && !Objects.equals(className.trim(), view.getClassName())) {
            return false;
        }
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(view.getUsername(), normalizedKeyword)
                || contains(view.getRealName(), normalizedKeyword)
                || contains(view.getEmail(), normalizedKeyword)
                || contains(view.getPhone(), normalizedKeyword)
                || contains(view.getStudentNo(), normalizedKeyword)
                || contains(view.getGrade(), normalizedKeyword)
                || contains(view.getClassName(), normalizedKeyword)
                || contains(view.getGuardian(), normalizedKeyword);
    }

    private boolean matchesTeacher(TeacherPersonnelVO view, String keyword, String department) {
        if (StringUtils.hasText(department) && !Objects.equals(department.trim(), view.getDepartment())) {
            return false;
        }
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(view.getUsername(), normalizedKeyword)
                || contains(view.getRealName(), normalizedKeyword)
                || contains(view.getEmail(), normalizedKeyword)
                || contains(view.getPhone(), normalizedKeyword)
                || contains(view.getTeacherNo(), normalizedKeyword)
                || contains(view.getDepartment(), normalizedKeyword)
                || view.getSubjects().stream().anyMatch(subject -> contains(subject, normalizedKeyword));
    }

    private boolean contains(String value, String keyword) {
        return StringUtils.hasText(value) && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private UserEntity getUser(String userId, String role) {
        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getRole, role)
                .last("LIMIT 1"));
        if (user == null) {
            throw new BusinessException(404, role + " not found");
        }
        return user;
    }

    private void validateStudentRequest(StudentPersonnelSaveRequestDTO request) {
        if (request == null) {
            throw new BusinessException(422, "request body is required");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(422, "username is required");
        }
        if (!request.getUsername().trim().startsWith("stu")) {
            throw new BusinessException(422, "student username must start with stu");
        }
        if (!StringUtils.hasText(request.getRealName())) {
            throw new BusinessException(422, "realName is required");
        }
        if (!StringUtils.hasText(request.getStatus())) {
            throw new BusinessException(422, "status is required");
        }
        if (!StringUtils.hasText(request.getStudentNo())) {
            throw new BusinessException(422, "studentNo is required");
        }
        if (!StringUtils.hasText(request.getGrade())) {
            throw new BusinessException(422, "grade is required");
        }
        if (!StringUtils.hasText(request.getClassName())) {
            throw new BusinessException(422, "className is required");
        }
        normalizeStatus(request.getStatus());
    }

    private void validateTeacherRequest(TeacherPersonnelSaveRequestDTO request) {
        if (request == null) {
            throw new BusinessException(422, "request body is required");
        }
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(422, "username is required");
        }
        if (!request.getUsername().trim().startsWith("tch")) {
            throw new BusinessException(422, "teacher username must start with tch");
        }
        if (!StringUtils.hasText(request.getRealName())) {
            throw new BusinessException(422, "realName is required");
        }
        if (!StringUtils.hasText(request.getStatus())) {
            throw new BusinessException(422, "status is required");
        }
        if (!StringUtils.hasText(request.getTeacherNo())) {
            throw new BusinessException(422, "teacherNo is required");
        }
        if (!StringUtils.hasText(request.getDepartment())) {
            throw new BusinessException(422, "department is required");
        }
        if (normalizeSubjects(request.getSubjects()).isEmpty()) {
            throw new BusinessException(422, "subjects is required");
        }
        normalizeStatus(request.getStatus());
    }

    private void ensureUsernameUnique(String username, String selfId) {
        UserEntity existing = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, normalize(username))
                .last("LIMIT 1"));
        if (existing != null && !Objects.equals(existing.getId(), selfId)) {
            throw new BusinessException(409, "username duplicated");
        }
    }

    private void ensureStudentNoUnique(String studentNo, String selfId) {
        StudentProfileEntity existing = studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfileEntity>()
                .eq(StudentProfileEntity::getStudentNo, normalize(studentNo))
                .last("LIMIT 1"));
        if (existing != null && !Objects.equals(existing.getStudentId(), selfId)) {
            throw new BusinessException(409, "studentNo duplicated");
        }
    }

    private void ensureTeacherNoUnique(String teacherNo, String selfId) {
        TeacherProfileEntity existing = teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfileEntity>()
                .eq(TeacherProfileEntity::getTeacherNo, normalize(teacherNo))
                .last("LIMIT 1"));
        if (existing != null && !Objects.equals(existing.getTeacherId(), selfId)) {
            throw new BusinessException(409, "teacherNo duplicated");
        }
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

    private List<String> normalizeSubjects(Collection<String> subjects) {
        if (subjects == null) {
            return Collections.emptyList();
        }
        return subjects.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private String writeSubjects(Collection<String> subjects) {
        List<String> normalizedSubjects = normalizeSubjects(subjects);
        try {
            return objectMapper.writeValueAsString(normalizedSubjects);
        } catch (Exception ex) {
            throw new BusinessException(500, "failed to save subjects");
        }
    }

    private List<String> readSubjects(String subjectsJson) {
        if (!StringUtils.hasText(subjectsJson)) {
            return Collections.emptyList();
        }
        try {
            List<String> values = objectMapper.readValue(subjectsJson, new TypeReference<List<String>>() {
            });
            return normalizeSubjects(values);
        } catch (Exception ex) {
            return splitSubjects(subjectsJson);
        }
    }

    private List<String> splitSubjects(String rawSubjects) {
        if (!StringUtils.hasText(rawSubjects)) {
            return Collections.emptyList();
        }
        String normalized = rawSubjects.replace('|', ',');
        List<String> subjects = new ArrayList<>();
        for (String item : normalized.split(",")) {
            if (StringUtils.hasText(item)) {
                subjects.add(item.trim());
            }
        }
        return normalizeSubjects(subjects);
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
                && isBlank(row.getRealName())
                && isBlank(row.getEmail())
                && isBlank(row.getPhone())
                && isBlank(row.getStatus())
                && isBlank(row.getStudentNo())
                && isBlank(row.getGrade())
                && isBlank(row.getClassName())
                && isBlank(row.getGuardian())
                && isBlank(row.getAvatar());
    }

    private boolean isTeacherRowEmpty(TeacherImportRow row) {
        return row == null || isBlank(row.getUsername())
                && isBlank(row.getRealName())
                && isBlank(row.getEmail())
                && isBlank(row.getPhone())
                && isBlank(row.getStatus())
                && isBlank(row.getTeacherNo())
                && isBlank(row.getDepartment())
                && isBlank(row.getSubjects())
                && isBlank(row.getAvatar());
    }

    private boolean isBlank(String value) {
        return !StringUtils.hasText(value);
    }

    private void addFailedRow(PersonnelImportResultDTO result, int rowNumber, String reason) {
        result.setSkippedCount(result.getSkippedCount() + 1);
        result.getFailedRows().add(new PersonnelImportFailureDTO(rowNumber, reason));
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

    private List<String> extractUserIds(List<UserEntity> users) {
        return users.stream().map(UserEntity::getId).toList();
    }
}
