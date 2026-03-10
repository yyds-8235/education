package com.example.education.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CourseChapterVO {

    private String id;
    private String courseId;
    private String title;
    private String description;
    private Integer order;
    private String createdAt;
    private List<CourseResourceVO> resources = new ArrayList<>();
}
