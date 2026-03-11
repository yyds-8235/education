package com.example.education.pojo.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class TeacherImportRow {

    @ExcelProperty("username")
    private String username;

    @ExcelProperty("realName")
    private String realName;

    @ExcelProperty("email")
    private String email;

    @ExcelProperty("phone")
    private String phone;

    @ExcelProperty("status")
    private String status;

    @ExcelProperty("teacherNo")
    private String teacherNo;

    @ExcelProperty("department")
    private String department;

    @ExcelProperty("subjects")
    private String subjects;

    @ExcelProperty("avatar")
    private String avatar;
}
