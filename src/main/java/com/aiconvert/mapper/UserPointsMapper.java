package com.aiconvert.mapper;

import com.aiconvert.entity.UserPoints;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserPointsMapper {
    UserPoints selectByUserId(@Param("userId") Long userId);
    int insert(UserPoints userPoints);
    int updateById(UserPoints userPoints);
} 