package com.example.education.pojo.vo;

import lombok.Data;

@Data
public class CourseResourceAccessVO {

    private String resourceId;
    private String fileName;
    private String url;
    private Integer expiresIn;
}
