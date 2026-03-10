package com.example.education.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TestSaveRequestDTO {

    @NotBlank(message = "courseId不能为空")
    private String courseId;

    @NotBlank(message = "title不能为空")
    private String title;

    private String description;

    @NotNull(message = "duration不能为空")
    @Min(value = 1, message = "duration必须大于0")
    private Integer duration;

    @NotNull(message = "showAnswer不能为空")
    private Boolean showAnswer;

    @Valid
    @NotEmpty(message = "questions至少包含1题")
    private List<TestQuestionRequestDTO> questions = new ArrayList<>();
}
