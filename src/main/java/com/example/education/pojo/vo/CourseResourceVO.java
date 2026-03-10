package com.example.education.pojo.vo;

import lombok.Data;

@Data
public class CourseResourceVO {

    private String id;
    private String chapterId;
    private String name;
    private String type;
    private String url;
    private String bucketName;
    private String objectKey;
    private Long size;
    private Integer duration;
    private Integer order;
    private String createdAt;
}
