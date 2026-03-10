package com.example.education;

import com.example.education.config.AuthProperties;
import com.example.education.config.MinioProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.example.education.mapper")
@EnableConfigurationProperties({MinioProperties.class, AuthProperties.class})
public class EducationApplication {

    public static void main(String[] args) {
        SpringApplication.run(EducationApplication.class, args);
    }

}
