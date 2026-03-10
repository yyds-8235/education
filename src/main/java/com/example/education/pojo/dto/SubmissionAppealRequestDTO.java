package com.example.education.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmissionAppealRequestDTO {

    @NotBlank(message = "reason不能为空")
    private String reason;
}
