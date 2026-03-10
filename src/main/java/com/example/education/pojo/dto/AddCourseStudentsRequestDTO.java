package com.example.education.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AddCourseStudentsRequestDTO {

    @NotEmpty(message = "studentIds不能为空")
    private List<@NotBlank(message = "studentId不能为空") String> studentIds;
}
