package com.example.education.pojo.vo;

import lombok.Data;

@Data
public class CourseUploadResourceVO {

    private String id;
    private String name;
    private String type;
    private String url;
    private String bucketName;
    private String objectKey;
    private Long size;
}
