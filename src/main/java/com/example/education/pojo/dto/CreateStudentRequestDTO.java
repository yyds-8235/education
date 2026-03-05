package com.example.education.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateStudentRequestDTO {

    @NotBlank(message = "studentNo不能为空")
    private String studentNo;

    @NotBlank(message = "name不能为空")
    private String name;

    @NotBlank(message = "username不能为空")
    private String username;

    @NotBlank(message = "password不能为空")
    private String password;

    @NotBlank(message = "grade不能为空")
    private String grade;

    @NotBlank(message = "class不能为空")
    @JsonProperty("class")
    private String clazz;

    private String guardian;

    @NotBlank(message = "povertyLevel不能为空")
    private String povertyLevel;

    @NotNull(message = "isSponsored不能为空")
    private Boolean isSponsored;

    @NotBlank(message = "householdType不能为空")
    private String householdType;

    @NotNull(message = "isLeftBehind不能为空")
    private Boolean isLeftBehind;

    @NotNull(message = "isDisabled不能为空")
    private Boolean isDisabled;

    @NotNull(message = "isSingleParent不能为空")
    private Boolean isSingleParent;

    @NotNull(message = "isKeyConcern不能为空")
    private Boolean isKeyConcern;

    @NotNull(message = "canView不能为空")
    private Boolean canView;

    @NotNull(message = "canEdit不能为空")
    private Boolean canEdit;

    private String email;
    private String phone;
}

