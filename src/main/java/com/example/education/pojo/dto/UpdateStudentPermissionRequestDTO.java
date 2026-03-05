package com.example.education.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateStudentPermissionRequestDTO {

    @NotNull(message = "canView不能为空")
    private Boolean canView;

    @NotNull(message = "canEdit不能为空")
    private Boolean canEdit;
}

