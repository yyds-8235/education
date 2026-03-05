package com.example.education.pojo.dto;

import com.example.education.pojo.vo.UserVO;
import lombok.Data;

@Data
public class LoginResponseDataDTO {

    private String token;
    private Long expiresIn;
    private UserVO user;
}

