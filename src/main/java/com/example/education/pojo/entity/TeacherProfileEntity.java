package com.example.education.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("teacher_profiles")
public class TeacherProfileEntity {

    @TableId(value = "teacher_id", type = IdType.INPUT)
    private String teacherId;

    @TableField("teacher_no")
    private String teacherNo;

    @TableField("department")
    private String department;

    @TableField("subjects_json")
    private String subjectsJson;
}
