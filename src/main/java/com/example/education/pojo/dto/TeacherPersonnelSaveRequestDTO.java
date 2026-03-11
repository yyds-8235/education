package com.example.education.pojo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
public class TeacherPersonnelSaveRequestDTO {

    private String username;

    @JsonAlias({"realName", "name"})
    private String realName;

    private String email;

    private String phone;

    private String avatar;

    private String status;

    private String teacherNo;

    private String department;

    private List<String> subjects;

    private String password;
}
