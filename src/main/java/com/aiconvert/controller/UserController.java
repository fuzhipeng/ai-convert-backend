package com.aiconvert.controller;

import com.aiconvert.common.ApiResponse;
import com.aiconvert.entity.User;
import com.aiconvert.entity.UserPoints;
import com.aiconvert.mapper.UserPointsMapper;
import com.aiconvert.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserPointsMapper userPointsMapper;
    
    /**
     * 高效获取用户积分（通过google_id）
     * 直接从User表查询，不再需要联表
     * 
     * @param googleId 用户的Google ID
     * @return 用户积分和基本信息
     */
    @GetMapping("/points")
    public ApiResponse<?> getUserPointsByGoogleIdEfficient(@RequestParam("googleId") String googleId) {
        logger.info("高效获取用户积分，googleId: {}", googleId);
        
        if (googleId == null || googleId.isEmpty()) {
            return ApiResponse.error(400, "无效的Google ID");
        }
        
        try {
            // 直接从User表查询所有所需数据
            Map<String, Object> result = userPointsMapper.selectPointsByGoogleId(googleId);
            
            if (result == null || result.isEmpty()) {
                logger.warn("未找到用户记录, googleId: {}", googleId);
                return ApiResponse.error(404, "未找到用户记录");
            }
            
            // 检查积分是否为null
            if (result.get("points") == null) {
                logger.warn("用户积分不存在, googleId: {}", googleId);
                return ApiResponse.error(404, "用户积分不存在");
            }
            
            logger.info("成功获取用户积分, googleId: {}, points: {}", googleId, result.get("points"));
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            logger.error("获取用户积分失败", e);
            return ApiResponse.error(500, "获取用户积分失败: " + e.getMessage());
        }
    }
} 