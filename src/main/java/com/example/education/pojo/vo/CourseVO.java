package com.example.education.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseVO {

    private String id;
    private String name;
    private String description;
    private String grade;

    @JsonProperty("class")
    private String clazz;

    private String subject;
    private String teacherId;
    private String teacherName;
    private String visibility;
    private String coverImage;
    private Integer studentCount;
    private String status;
    private String createdAt;
    private String updatedAt;
    private List<CourseChapterVO> chapters = new ArrayList<>();
}
