package com.example.education.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_profiles")
public class StudentProfileEntity {

    @TableId(value = "student_id", type = IdType.INPUT)
    private String studentId;

    @TableField("student_no")
    private String studentNo;

    @TableField("grade")
    private String grade;

    @TableField("class_name")
    private String className;

    @TableField("guardian")
    private String guardian;

    @TableField("poverty_level")
    private String povertyLevel;

    @TableField("is_sponsored")
    private Boolean isSponsored;

    @TableField("household_type")
    private String householdType;

    @TableField("is_left_behind")
    private Boolean isLeftBehind;

    @TableField("is_disabled")
    private Boolean isDisabled;

    @TableField("is_single_parent")
    private Boolean isSingleParent;

    @TableField("is_key_concern")
    private Boolean isKeyConcern;

    @TableField("can_view")
    private Boolean canView;

    @TableField("can_edit")
    private Boolean canEdit;

    @TableField("synced_at")
    private LocalDateTime syncedAt;
}

