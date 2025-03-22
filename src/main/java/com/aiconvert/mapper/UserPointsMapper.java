package com.aiconvert.mapper;

import com.aiconvert.entity.UserPoints;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface UserPointsMapper {
    UserPoints selectByUserId(@Param("userId") Long userId);
    int insert(UserPoints userPoints);
    int updateById(UserPoints userPoints);
    
    /**
     * 通过googleId直接查询用户积分（直接从User表查询）
     * @param googleId 用户的谷歌ID
     * @return 包含用户和积分信息的Map
     */
    Map<String, Object> selectPointsByGoogleId(@Param("googleId") String googleId);
} 