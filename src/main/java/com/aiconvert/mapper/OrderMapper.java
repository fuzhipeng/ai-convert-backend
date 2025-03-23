package com.aiconvert.mapper;

import com.aiconvert.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {
    int insert(Order order);
    
    Order selectByOrderId(@Param("orderId") String orderId);
    
    Order selectByRequestId(@Param("requestId") String requestId);
    
    List<Order> selectByUserId(@Param("userId") String userId);
    
    int updateStatus(@Param("orderId") String orderId, @Param("status") Integer status);
    
    int updateById(Order order);
} 