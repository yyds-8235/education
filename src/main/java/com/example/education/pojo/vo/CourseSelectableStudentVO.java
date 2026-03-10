package com.example.education.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CourseSelectableStudentVO {

    private String id;
    private String username;
    private String realName;
    private String studentNo;
    private String grade;

    @JsonProperty("class")
    private String clazz;
}
