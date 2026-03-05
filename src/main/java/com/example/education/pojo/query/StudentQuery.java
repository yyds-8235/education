package com.example.education.pojo.query;

import lombok.Data;

@Data
public class StudentQuery {

    private Long page = 1L;
    private Long pageSize = 10L;
    private String keyword;
    private String grade;
    private String className;
    private String archiveFilter;
    private Boolean canView;
    private Boolean canEdit;
}

