package com.example.education.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class TeacherPersonnelVO {

    private String id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String avatar;
    private String status;
    private String createdAt;
    private String teacherNo;
    private String department;
    private List<String> subjects;
}
