package com.example.education.pojo.model;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class StudentImportRow {

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

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("年级")
    private String grade;

    @ExcelProperty("班级")
    private String className;

    @ExcelProperty("监护人")
    private String guardian;

    @ExcelProperty("困难等级")
    private String povertyLevel;

    @ExcelProperty("是否资助")
    private Boolean isSponsored;

    @ExcelProperty("户口类型")
    private String householdType;

    @ExcelProperty("是否留守")
    private Boolean isLeftBehind;

    @ExcelProperty("是否残疾")
    private Boolean isDisabled;

    @ExcelProperty("是否单亲")
    private Boolean isSingleParent;

    @ExcelProperty("是否重点关注")
    private Boolean isKeyConcern;

    @ExcelProperty("可查看")
    private Boolean canView;

    @ExcelProperty("可编辑")
    private Boolean canEdit;

    @ExcelProperty("头像")
    private String avatar;
}
