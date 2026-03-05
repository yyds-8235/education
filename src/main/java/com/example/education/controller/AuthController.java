package com.example.education.controller;

import com.example.education.common.api.ApiResponse;
import com.example.education.pojo.dto.LoginRequestDTO;
import com.example.education.pojo.dto.LoginResponseDataDTO;
import com.example.education.pojo.vo.UserVO;
import com.example.education.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponseDataDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me(@RequestHeader(value = "Authorization", required = false) String token) {
        return ApiResponse.success(authService.me(token));
    }

    @PostMapping("/logout")
    public ApiResponse<Object> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.logout(token);
        return ApiResponse.success(Collections.emptyMap());
    }
}

