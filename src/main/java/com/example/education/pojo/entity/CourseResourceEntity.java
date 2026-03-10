package com.example.education.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_resources")
public class CourseResourceEntity {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField("chapter_id")
    private String chapterId;

    @TableField("name")
    private String name;

    @TableField("type")
    private String type;

    @TableField("url")
    private String url;

    @TableField("bucket_name")
    private String bucketName;

    @TableField("object_key")
    private String objectKey;

    @TableField("size")
    private Long size;

    @TableField("duration")
    private Integer duration;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
