package com.example.education.pojo.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StudentImportRow {

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

    @ExcelProperty("studentNo")
    private String studentNo;

    @ExcelProperty("grade")
    private String grade;

    @ExcelProperty("className")
    private String className;

    @ExcelProperty("guardian")
    private String guardian;

    @ExcelProperty("avatar")
    private String avatar;
}
