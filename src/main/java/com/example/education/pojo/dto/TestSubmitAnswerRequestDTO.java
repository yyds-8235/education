package com.example.education.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestSubmitAnswerRequestDTO {

    @NotBlank(message = "questionId不能为空")
    private String questionId;

    private String answer;
}
