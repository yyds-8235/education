package com.example.education.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.education.common.exception.BusinessException;
import com.example.education.common.util.TimeUtils;
import com.example.education.config.AuthProperties;
import com.example.education.mapper.UserMapper;
import com.example.education.pojo.dto.LoginRequestDTO;
import com.example.education.pojo.dto.LoginResponseDataDTO;
import com.example.education.pojo.entity.UserEntity;
import com.example.education.pojo.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_STUDENT = "student";
    private static final String ROLE_TEACHER = "teacher";

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenSessionService tokenSessionService;
    private final AuthProperties authProperties;

    public LoginResponseDataDTO login(LoginRequestDTO request) {
        String expectedRole = parseRoleByUsername(request.getUsername());

        UserEntity user = userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, request.getUsername())
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(403, "账号已禁用");
        }

        if (!expectedRole.equals(user.getRole())) {
            throw new BusinessException(403, "账号角色异常");
        }

        Long expiresIn = authProperties.getTokenExpireSeconds();
        String token = tokenSessionService.createSession(user.getId(), expiresIn);

        LoginResponseDataDTO responseData = new LoginResponseDataDTO();
        responseData.setToken(token);
        responseData.setExpiresIn(expiresIn);
        responseData.setUser(toUserView(user));
        return responseData;
    }

    public UserVO me(String rawToken) {
        UserEntity user = requireLogin(rawToken);
        return toUserView(user);
    }

    public void logout(String rawToken) {
        String token = normalizeToken(rawToken);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        tokenSessionService.removeSession(token);
    }

    public UserEntity requireLogin(String rawToken) {
        String token = normalizeToken(rawToken);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(401, "未登录或登录已过期");
        }

        String userId = tokenSessionService.getUserId(token)
                .orElseThrow(() -> new BusinessException(401, "未登录或登录已过期"));

        UserEntity user = userMapper.selectById(userId);
        if (user == null || !"active".equals(user.getStatus())) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return user;
    }

    public UserEntity requireAdmin(String rawToken) {
        UserEntity user = requireLogin(rawToken);
        if (!ROLE_ADMIN.equals(user.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }
        return user;
    }

    public UserEntity requireTeacher(String rawToken) {
        UserEntity user = requireLogin(rawToken);
        if (!ROLE_TEACHER.equals(user.getRole())) {
            throw new BusinessException(403, "无权限访问");
        }
        return user;
    }

    private String parseRoleByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(422, "账号前缀不合法");
        }
        if ("admin".equals(username)) {
            return ROLE_ADMIN;
        }
        if (username.startsWith("stu")) {
            return ROLE_STUDENT;
        }
        if (username.startsWith("tch")) {
            return ROLE_TEACHER;
        }
        throw new BusinessException(422, "账号前缀不合法");
    }

    private String normalizeToken(String rawToken) {
        if (!StringUtils.hasText(rawToken)) {
            return null;
        }
        String token = rawToken.trim();
        if (token.startsWith("Bearer ")) {
            return token.substring(7).trim();
        }
        return token;
    }

    public UserVO toUserView(UserEntity user) {
        UserVO view = new UserVO();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setRealName(user.getRealName());
        view.setEmail(user.getEmail());
        view.setPhone(user.getPhone());
        view.setAvatar(user.getAvatar());
        view.setRole(user.getRole());
        view.setStatus(user.getStatus());
        view.setCreatedAt(TimeUtils.toIsoUtc(user.getCreatedAt()));
        view.setUpdatedAt(TimeUtils.toIsoUtc(user.getUpdatedAt()));
        return view;
    }
}
