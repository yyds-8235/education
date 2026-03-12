package com.example.education.pojo.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class TeacherImportRow {

    @ExcelProperty("用户名")
    private String username;

    @ExcelProperty("密码")
    private String password;

    @ExcelProperty("姓名")
    private String realName;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("工号")
    private String teacherNo;

    @ExcelProperty("部门")
    private String department;

    @ExcelProperty("学科")
    private String subjects;

    @ExcelProperty("头像")
    private String avatar;
}
