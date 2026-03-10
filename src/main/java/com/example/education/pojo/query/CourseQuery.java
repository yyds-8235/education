package com.example.education.pojo.query;

import lombok.Data;

@Data
public class CourseQuery {

    private Long page;
    private Long pageSize;
    private String keyword;
    private String grade;
    private String className;
    private String subject;
    private String status;
    private String scope;
}
