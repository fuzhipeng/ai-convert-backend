package com.aiconvert.controller;

import com.aiconvert.common.ApiResponse;
import com.aiconvert.entity.Order;
import com.aiconvert.entity.Subscription;
import com.aiconvert.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private PaymentService paymentService;
    
    /**
     * 创建结账会话
     * 
     * @param request 包含产品ID、变体ID和用户ID的请求
     * @return 结账URL和请求ID
     */
    @PostMapping("/checkout/create")
    public ApiResponse<?> createCheckoutSession(
            @RequestBody Map<String, Object> request) {
        
        String productId = (String) request.get("productId");
        String requestId = (String) request.get("requestId");
        String metadata = request.containsKey("metadata") ? request.get("metadata").toString() : null;
        
        // 从请求体获取userId
        String userId = null;
        if (request.containsKey("userId")) {
            try {
                userId = request.get("userId").toString();
            } catch (NumberFormatException e) {
                return ApiResponse.error(400, "无效的用户ID格式");
            }
        }
        
        logger.info("创建结账会话: userId={}, productId={}, requestId={}", userId, productId, requestId);
        
        if (productId == null || productId.isEmpty()) {
            return ApiResponse.error(400, "产品ID不能为空");
        }
    
        if (userId == null) {
            return ApiResponse.error(400, "用户ID不能为空");
        }
        
        try {
            // 创建结账会话，保存requestId作为临时标识，实际订单ID将通过webhook更新
            Map<String, Object> result = paymentService.createCheckoutSession(userId, productId, requestId, metadata);
            logger.info("Creem API响应: {}", result);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("创建结账会话失败", e);
            return ApiResponse.error(500, "创建结账会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证支付状态
     * 
     * @param checkoutId 结账会话ID
     * @return 订单详情
     */
    @GetMapping("/orders/{checkoutId}")
    public ApiResponse<?> verifyPaymentStatus(@PathVariable("checkoutId") String checkoutId) {
        logger.info("验证支付状态: checkoutId={}", checkoutId);
        
        try {
            Order order = paymentService.verifyPaymentStatus(checkoutId);
            
            if (order == null) {
                return ApiResponse.error(404, "订单不存在");
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("checkoutId", order.getOrderId());
            result.put("productName", order.getProductName());
            result.put("amount", order.getAmount());
            result.put("status", order.getStatus());
            result.put("payTime", order.getPayTime());
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("验证支付状态失败", e);
            return ApiResponse.error(500, "验证支付状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 查询用户订阅
     * 
     * @param userId 用户ID（从会话获取）
     * @return 订阅信息
     */
    @GetMapping("/subscriptions")
    public ApiResponse<?> getUserSubscription(@RequestParam("userId") String userId) {
        logger.info("查询用户订阅: userId={}", userId);
        
        try {
            Subscription subscription = paymentService.getUserSubscription(userId);
            
            if (subscription == null) {
                return ApiResponse.success(null); // 用户没有订阅
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("subscriptionId", subscription.getSubscriptionId());
            result.put("planName", subscription.getPlanName());
            result.put("status", subscription.getStatus());
            result.put("startTime", subscription.getStartTime());
            result.put("endTime", subscription.getEndTime());
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("查询用户订阅失败", e);
            return ApiResponse.error(500, "查询用户订阅失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消订阅
     * 
     * @param userId 用户ID（从会话获取）
     * @param subscriptionId 订阅ID
     * @return 处理结果
     */
    @PostMapping("/subscriptions/{subscriptionId}/cancel")
    public ApiResponse<?> cancelSubscription(
            @RequestAttribute("userId") String userId,
            @PathVariable("subscriptionId") String subscriptionId) {
        
        logger.info("取消订阅: userId={}, subscriptionId={}", userId, subscriptionId);
        
        try {
            boolean success = paymentService.cancelSubscription(userId, subscriptionId);
            
            if (success) {
                return ApiResponse.success("订阅已取消");
            } else {
                return ApiResponse.error(400, "取消订阅失败");
            }
        } catch (Exception e) {
            logger.error("取消订阅失败", e);
            return ApiResponse.error(500, "取消订阅失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理支付成功回调
     * 需要将此URL设置为Creem的success_url
     */
    @GetMapping("/success")
    public ApiResponse<?> handlePaymentSuccess(
            @RequestParam("checkout_id") String checkoutId,
            @RequestParam(value = "userId", required = false) String userId) {
        
        logger.info("支付成功回调: checkoutId={}, userId={}", checkoutId, userId);
        
        try {
            // 验证订单状态
            Order order = paymentService.verifyPaymentStatus(checkoutId);
            
            if (order == null) {
                return ApiResponse.error(404, "订单不存在");
            }
            
            return ApiResponse.success("支付成功");
        } catch (Exception e) {
            logger.error("处理支付成功回调异常", e);
            return ApiResponse.error(500, "处理支付成功回调异常: " + e.getMessage());
        }
    }
    
    /**
     * 支付回调接口
     * 需要将此URL配置为Creem的webhook endpoint
     * 
     * @param payload 回调数据
     * @return 处理结果
     */
    @PostMapping("/webhook")
    public ApiResponse<?> handlePaymentWebhook(@RequestBody String payload) {
        logger.info("接收支付回调");
        
        try {
            boolean success = paymentService.handlePaymentWebhook(payload);
            
            if (success) {
                return ApiResponse.success("处理成功");
            } else {
                // 返回200状态码但提示处理失败，避免支付平台重复回调
                return ApiResponse.success("处理失败，但不需要重试");
            }
        } catch (Exception e) {
            logger.error("处理支付回调失败", e);
            // 返回200状态码但提示处理异常，避免支付平台重复回调
            return ApiResponse.success("处理异常: " + e.getMessage());
        }
    }
} 