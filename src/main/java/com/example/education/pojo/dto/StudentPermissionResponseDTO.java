package com.example.education.pojo.dto;

import lombok.Data;

@Data
public class StudentPermissionResponseDTO {

    private String studentId;
    private Boolean canView;
    private Boolean canEdit;
    private String updatedAt;
}

