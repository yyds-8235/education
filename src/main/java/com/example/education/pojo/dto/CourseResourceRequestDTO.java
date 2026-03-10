package com.example.education.pojo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseResourceRequestDTO {

    @NotBlank(message = "resource.name不能为空")
    private String name;

    @NotBlank(message = "resource.type不能为空")
    private String type;

    @NotBlank(message = "resource.url不能为空")
    private String url;

    private String bucketName;

    private String objectKey;

    @NotNull(message = "resource.size不能为空")
    @Min(value = 0, message = "resource.size不能小于0")
    private Long size;

    private Integer duration;
}
