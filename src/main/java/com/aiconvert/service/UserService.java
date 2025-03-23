package com.aiconvert.service;

import com.aiconvert.entity.User;
import com.aiconvert.entity.UserPoints;
import com.aiconvert.mapper.UserMapper;
import com.aiconvert.mapper.UserPointsMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserPointsMapper userPointsMapper;
    
    /**
     * 通过Google ID查找用户
     */
    public User findByGoogleId(String googleId) {
        return userMapper.selectByGoogleId(googleId);
    }
    
    /**
     * 创建或更新Google用户
     */
    @Transactional
    public Map<String, Object> createOrUpdateGoogleUser(String googleId, String email, String name, String picture) {
        Date now = new Date();
        User user = findByGoogleId(googleId);
        boolean isNewUser = false;
        
        if (user == null) {
            // 创建新用户
            user = new User();
            user.setGoogleId(googleId);
            user.setEmail(email);
            user.setName(name);
            user.setPicture(picture);
            user.setCreateTime(now);
            user.setUpdateTime(now);
            user.setStatus(1); // 正常状态
            user.setPoints(30); // 初始30积分
            
            userMapper.insert(user);
            logger.info("创建新Google用户: {}", email);
            
            // 为了兼容性，仍然初始化用户积分表
            UserPoints userPoints = new UserPoints();
            userPoints.setUserId(user.getId());
            userPoints.setPoints(30); // 初始30积分
            userPoints.setPointsChange(30); // 初始积分变动
            userPoints.setRemark("新用户初始积分");
            userPoints.setCreateTime(now);
            userPoints.setUpdateTime(now);
            
            userPointsMapper.insert(userPoints);
            logger.info("初始化用户积分: userId={}, points=30", user.getId());
            
            isNewUser = true;
        } else {
            // 更新现有用户信息
            user.setEmail(email);
            user.setName(name);
            user.setPicture(picture);
            user.setUpdateTime(now);
            
            userMapper.updateById(user);
            logger.info("更新Google用户信息: {}", email);
        }
        
        // 获取用户积分
        UserPoints userPoints = userPointsMapper.selectLatestByUserId(user.getId());
        
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("points", user.getPoints() != null ? user.getPoints() : (userPoints != null ? userPoints.getPoints() : 0));
        result.put("isNewUser", isNewUser);
        
        return result;
    }
    
    /**
     * 更新用户积分
     * @param userId 用户ID
     * @param pointsToAdd 要添加的积分数量（可以为负数，表示减少积分）
     * @param remark 积分变动说明
     * @return 更新后的积分
     */
    @Transactional
    public int updateUserPoints(String userId, int pointsToAdd, String remark) {
        if (userId == null || pointsToAdd == 0) {
            return 0;
        }
        
        logger.info("更新用户积分: userId={}, pointsToAdd={}, remark={}", userId, pointsToAdd, remark);
        
        // 查询用户
        User user = userMapper.selectByGoogleId(userId);
        if (user == null) {
            logger.warn("用户不存在: userId={}", userId);
            return 0;
        }
        
        // 当前积分
        int currentPoints = user.getPoints() != null ? user.getPoints() : 0;
        
        // 新积分（不允许为负数）
        int newPoints = Math.max(0, currentPoints + pointsToAdd);
        
        // 更新User表中的积分
        user.setPoints(newPoints);
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
        
        // 在UserPoints表中新增一条积分变动记录
        UserPoints userPoints = new UserPoints();
        userPoints.setUserId(user.getId());
        userPoints.setPoints(pointsToAdd);  // 当前总积分
        userPoints.setCreateTime(new Date());
        userPoints.setUpdateTime(new Date());
        
        userPointsMapper.insert(userPoints);
        
        logger.info("用户积分更新成功: userId={}, oldPoints={}, newPoints={}", userId, currentPoints, newPoints);
        
        return newPoints;
    }
    
    /**
     * 更新用户积分（无备注版本，向后兼容）
     * @param userId 用户ID
     * @param pointsToAdd 要添加的积分数量（可以为负数，表示减少积分）
     * @return 更新后的积分
     */
    @Transactional
    public int updateUserPoints(String userId, int pointsToAdd) {
        return updateUserPoints(userId, pointsToAdd, "系统积分变动");
    }
} 