package com.example.education.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseSaveRequestDTO {

    @NotBlank(message = "name不能为空")
    private String name;

    private String description;

    @NotBlank(message = "grade不能为空")
    private String grade;

    @NotBlank(message = "class不能为空")
    @JsonProperty("class")
    private String clazz;

    @NotBlank(message = "subject不能为空")
    private String subject;

    @NotBlank(message = "visibility不能为空")
    private String visibility;

    @NotBlank(message = "status不能为空")
    private String status;

    private String coverImage;

    @Valid
    private List<CourseChapterRequestDTO> chapters = new ArrayList<>();
}
