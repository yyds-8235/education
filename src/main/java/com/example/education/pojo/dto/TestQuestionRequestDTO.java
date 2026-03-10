package com.example.education.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestQuestionRequestDTO {

    @NotBlank(message = "question.type不能为空")
    private String type;

    @NotBlank(message = "question.content不能为空")
    private String content;

    @Valid
    private List<TestQuestionOptionRequestDTO> options = new ArrayList<>();

    private String answer;

    @NotNull(message = "question.score不能为空")
    @Min(value = 1, message = "question.score必须大于0")
    private Integer score;

    private String analysis;
}
