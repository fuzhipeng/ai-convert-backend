package com.aiconvert.mapper;

import com.aiconvert.entity.Subscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SubscriptionMapper {
    int insert(Subscription subscription);
    
    Subscription selectByUserId(@Param("userId") String userId);
    
    Subscription selectBySubscriptionId(@Param("subscriptionId") String subscriptionId);
    
    int updateStatus(@Param("subscriptionId") String subscriptionId, @Param("status") String status);
    
    int updateById(Subscription subscription);
} 