package com.example.education.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.education.pojo.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}

