package com.example.education.pojo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseChapterRequestDTO {

    @NotBlank(message = "chapter.title不能为空")
    private String title;

    private String description;

    @Valid
    private List<CourseResourceRequestDTO> resources = new ArrayList<>();
}
