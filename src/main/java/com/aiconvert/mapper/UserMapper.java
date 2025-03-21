package com.aiconvert.mapper;

import com.aiconvert.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User selectByGoogleId(@Param("googleId") String googleId);
    int insert(User user);
    int updateById(User user);
} 