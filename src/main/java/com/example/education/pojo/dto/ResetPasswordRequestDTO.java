package com.example.education.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequestDTO {

    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
