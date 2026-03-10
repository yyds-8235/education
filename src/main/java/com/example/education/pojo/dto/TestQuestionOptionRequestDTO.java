package com.example.education.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TestQuestionOptionRequestDTO {

    private String id;

    @NotBlank(message = "option.label不能为空")
    private String label;

    @NotBlank(message = "option.content不能为空")
    private String content;
}
