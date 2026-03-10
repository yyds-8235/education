package com.example.education.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TestGradeAnswerRequestDTO {

    @NotBlank(message = "questionId不能为空")
    private String questionId;

    @NotNull(message = "score不能为空")
    private Integer score;

    private String feedback;
}
