package com.aiconvert.service;

import com.aiconvert.entity.Order;
import com.aiconvert.entity.Subscription;

import java.util.Map;

public interface PaymentService {
    
    /**
     * 创建结账会话
     * @param userId 用户ID
     * @param productId 产品ID
     * @param requestId 变体ID
     * @param metadata 元数据(可选)
     * @return 包含结账URL的订单信息
     */
    Map<String, Object> createCheckoutSession(String userId, String productId, String requestId, String metadata);
    
    /**
     * 验证支付状态
     * @param orderId 订单ID
     * @return 订单详情
     */
    Order verifyPaymentStatus(String orderId);
    
    /**
     * 查询用户订阅
     * @param userId 用户ID
     * @return 订阅信息
     */
    Subscription getUserSubscription(String userId);
    
    /**
     * 取消订阅
     * @param userId 用户ID
     * @param subscriptionId 订阅ID
     * @return 是否成功
     */
    boolean cancelSubscription(String userId, String subscriptionId);
    
    /**
     * 处理支付回调
     * @param payload 回调数据
     * @return 处理结果
     */
    boolean handlePaymentWebhook(String payload);
} 