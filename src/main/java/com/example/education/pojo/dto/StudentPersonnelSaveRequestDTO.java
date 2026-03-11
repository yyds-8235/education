package com.example.education.pojo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class StudentPersonnelSaveRequestDTO {

    private String username;

    @JsonAlias({"realName", "name"})
    private String realName;

    private String email;

    private String phone;

    private String avatar;

    private String status;

    private String studentNo;

    private String grade;

    @JsonAlias({"className", "class"})
    private String className;

    private String guardian;

    private String password;
}
