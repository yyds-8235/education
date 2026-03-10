package com.example.education.pojo.query;

import lombok.Data;

@Data
public class TestQuery {

    private Long page;
    private Long pageSize;
    private String courseId;
    private String keyword;
}
