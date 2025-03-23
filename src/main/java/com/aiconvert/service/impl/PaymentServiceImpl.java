package com.aiconvert.service.impl;

import com.aiconvert.entity.Order;
import com.aiconvert.entity.Subscription;
import com.aiconvert.entity.User;
import com.aiconvert.mapper.OrderMapper;
import com.aiconvert.mapper.SubscriptionMapper;
import com.aiconvert.mapper.UserMapper;
import com.aiconvert.service.PaymentService;
import com.aiconvert.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Calendar;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PaymentServiceImpl implements PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    
    @Value("${payment.creem.api-key}")
    private String creamApiKey;
    
    @Value("${payment.creem.api-url}")
    private String creamApiUrl;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private SubscriptionMapper subscriptionMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private UserService userService;
    
    @Override
    @Transactional
    public Map<String, Object> createCheckoutSession(String userId, String productId, String requestId, String metadata) {
        logger.info("创建结账会话: userId={}, productId={}, requestId={}", userId, productId, requestId);
        
        // 创建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("product_id", productId);
        // 添加请求ID用于跟踪
        requestBody.put("request_id", requestId);
        
        // 设置成功回调URL（可根据实际情况修改）
        String successUrl = "https://docsmart.zeabur.app";
        requestBody.put("success_url", successUrl);
        
        // 添加元数据
        if (metadata != null && !metadata.isEmpty()) {
            try {
                // 假设metadata是JSON字符串
                @SuppressWarnings("unchecked")
                Map<String, Object> metadataMap = new ObjectMapper().readValue(metadata, Map.class);
                requestBody.put("metadata", metadataMap);
            } catch (Exception e) {
                logger.warn("元数据解析失败，将作为字符串处理: {}", e.getMessage());
                Map<String, Object> metadataMap = new HashMap<>();
                metadataMap.put("info", metadata);
                requestBody.put("metadata", metadataMap);
            }
        }
        
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", creamApiKey); // 使用x-api-key而不是Authorization
        
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        try {
            // 调用Creem API创建结账会话
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(
                creamApiUrl , // 正确的API端点
                requestEntity, 
                Map.class
            );
            logger.info("Creem API响应: {}", responseEntity);
            
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                Map<String, Object> responseBody = responseEntity.getBody();
                
                // 获取结账URL和会话ID
                String checkoutUrl = (String) responseBody.get("checkout_url");
                String checkoutId = (String) responseBody.get("checkout_id");
                
                // 创建订单记录，使用requestId而不是checkout_id
                Order order = new Order();
                order.setUserId(Long.valueOf(userId)); // 转换String类型的userId为Long类型
                // 临时设置订单ID，实际订单ID将通过webhook回调更新
                order.setOrderId(requestId); // 使用requestId作为临时订单ID
                order.setRequestId(requestId); // 保存requestId用于后续关联
                order.setProductId(productId);
                // 产品名称可能需要从其他地方获取，这里临时设置
                order.setProductName("产品" + productId);
                // 金额可能需要从产品信息中获取，将通过webhook更新
                order.setAmount(BigDecimal.ZERO); 
                order.setStatus(0); // 未支付
                order.setCheckoutUrl(checkoutUrl);
                order.setMetadata(metadata);
                order.setCreateTime(new Date());
                order.setUpdateTime(new Date());
                
                orderMapper.insert(order);
                
                // 返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("checkoutId", checkoutId);
                result.put("checkoutUrl", checkoutUrl);
                result.put("requestId", requestId);
                
                logger.info("创建结账会话成功: checkoutId={}, checkoutUrl={}, requestId={}", checkoutId, checkoutUrl, requestId);
                return result;
            } else {
                logger.error("创建结账会话失败: {}", responseEntity.getBody());
                throw new RuntimeException("创建结账会话失败");
            }
        } catch (Exception e) {
            logger.error("创建结账会话异常", e);
            throw new RuntimeException("创建结账会话异常: " + e.getMessage());
        }
    }
    
    @Override
    public Order verifyPaymentStatus(String orderId) {
        logger.info("验证支付状态: orderId={}", orderId);
        
        Order order = orderMapper.selectByOrderId(orderId);
        
        if (order == null) {
            logger.warn("订单不存在: orderId={}", orderId);
            return null;
        }
        
        // 已支付的订单直接返回
        if (order.getStatus() == 1) {
            return order;
        }
        
        // 查询Creem支付状态
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", creamApiKey);
        
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        
        try {
            // orderId在我们的系统中是Creem的checkout_id
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                creamApiUrl + "/checkouts/" + orderId, 
                org.springframework.http.HttpMethod.GET, 
                requestEntity, 
                Map.class
            );
            
            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                Map<String, Object> responseBody = responseEntity.getBody();
                
                String status = (String) responseBody.get("status");
                
                // 获取实际订单信息，如有需要
                if (responseBody.containsKey("order")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> orderInfo = (Map<String, Object>) responseBody.get("order");
                    if (orderInfo.containsKey("amount")) {
                        // 更新订单金额
                        BigDecimal amount = new BigDecimal(orderInfo.get("amount").toString());
                        order.setAmount(amount);
                    }
                    if (orderInfo.containsKey("order_id")) {
                        // 保存Creem的实际订单ID（与结账ID不同）
                        String creamOrderId = orderInfo.get("order_id").toString();
                        logger.info("获取到Creem订单ID: {}", creamOrderId);
                    }
                }
                
                // 更新订单状态
                if ("completed".equals(status) && order.getStatus() != 1) {
                    order.setStatus(1); // 已支付
                    order.setPayTime(new Date());
                    order.setUpdateTime(new Date());
                    orderMapper.updateById(order);
                    
                    // 如果是订阅商品，更新用户订阅
                    updateUserSubscription(order);
                } else if ("failed".equals(status) && order.getStatus() != 2) {
                    order.setStatus(2); // 支付失败
                    order.setUpdateTime(new Date());
                    orderMapper.updateById(order);
                }
            }
        } catch (Exception e) {
            logger.error("查询支付状态异常", e);
        }
        
        return order;
    }
    
    @Override
    public Subscription getUserSubscription(String userId) {
        logger.info("查询用户订阅: userId={}", userId);
        return subscriptionMapper.selectByUserId(userId);
    }
    
    @Override
    @Transactional
    public boolean cancelSubscription(String userId, String subscriptionId) {
        logger.info("取消订阅: userId={}, subscriptionId={}", userId, subscriptionId);
        
        Subscription subscription = subscriptionMapper.selectBySubscriptionId(subscriptionId);
        
        if (subscription == null || !subscription.getUserId().equals(userId)) {
            logger.warn("订阅不存在或不属于该用户: userId={}, subscriptionId={}", userId, subscriptionId);
            return false;
        }
        
        // 调用Creem API取消订阅
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + creamApiKey);
        
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                creamApiUrl + "/subscriptions/" + subscriptionId + "/cancel", 
                org.springframework.http.HttpMethod.POST, 
                requestEntity, 
                Map.class
            );
            
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                // 更新订阅状态
                subscription.setStatus("canceled");
                subscription.setUpdateTime(new Date());
                subscriptionMapper.updateById(subscription);
                return true;
            } else {
                logger.error("取消订阅失败: {}", responseEntity.getBody());
                return false;
            }
        } catch (Exception e) {
            logger.error("取消订阅异常", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public boolean handlePaymentWebhook(String payload) {
        logger.info("处理支付回调: {}", payload);
        
        try {
            // 解析回调数据
            @SuppressWarnings("unchecked")
            Map<String, Object> webhookData = new ObjectMapper().readValue(payload, Map.class);
            
            String eventType = (String) webhookData.get("eventType");
            logger.info("支付回调事件类型: {}", eventType);
            
            // webhook事件数据在object字段中，而不是data字段
            @SuppressWarnings("unchecked")
            Map<String, Object> eventObject = (Map<String, Object>) webhookData.get("object");
            
            if (eventObject == null) {
                logger.error("webhook数据格式错误，缺少object字段");
                return false;
            }
            
            // 处理订单完成事件
            if ("checkout.completed".equals(eventType)) {
                // checkout.completed事件，object就是checkout对象本身
                String checkoutId = (String) eventObject.get("id");
                String requestId = (String) eventObject.get("request_id");
                
                logger.info("订单完成事件: checkoutId={}, requestId={}", checkoutId, requestId);
                
                // 获取订单信息
                @SuppressWarnings("unchecked")
                Map<String, Object> orderInfo = (Map<String, Object>) eventObject.get("order");
                
                if (orderInfo == null) {
                    logger.warn("订单数据不完整，缺少order字段");
                    return false;
                }
                
                String orderId = (String) orderInfo.get("id");
                Object amountObj = orderInfo.get("amount");
                String status = (String) orderInfo.get("status");
                
                logger.info("订单信息: orderId={}, amount={}, status={}", orderId, amountObj, status);
                
                Order order = null;
                
                // 优先使用requestId查找订单
                if (requestId != null && !requestId.isEmpty()) {
                    // 通过requestId查找订单
                    order = orderMapper.selectByRequestId(requestId);
                    
                    if (order != null) {
                        logger.info("通过requestId找到订单: {}", requestId);
                        
                        // 更新真实的订单ID
                        if (orderId != null) {
                            logger.info("更新订单ID: 从{}更新为{}", order.getOrderId(), orderId);
                            order.setOrderId(orderId);
                        }
                        
                        // 更新金额
                        if (amountObj != null) {
                            BigDecimal amount;
                            if (amountObj instanceof Integer) {
                                amount = new BigDecimal((Integer) amountObj);
                            } else if (amountObj instanceof Long) {
                                amount = new BigDecimal((Long) amountObj);
                            } else if (amountObj instanceof Double) {
                                amount = BigDecimal.valueOf((Double) amountObj);
                            } else {
                                amount = new BigDecimal(amountObj.toString());
                            }
                            order.setAmount(amount);
                        }
                        
                        // 更新订单状态为已支付
                        if ("paid".equals(status)) {
                            order.setStatus(1); // 已支付
                            order.setPayTime(new Date());
                        } else if ("failed".equals(status)) {
                            order.setStatus(2); // 支付失败
                        }
                        
                        order.setUpdateTime(new Date());
                        orderMapper.updateById(order);
                        
                        // 获取产品信息
                        @SuppressWarnings("unchecked")
                        Map<String, Object> productInfo = (Map<String, Object>) eventObject.get("product");
                        if (productInfo != null) {
                            String productName = (String) productInfo.get("name");
                            if (productName != null && !productName.isEmpty()) {
                                order.setProductName(productName);
                                orderMapper.updateById(order);
                            }
                        }
                        
                        // 如果是订阅商品，更新用户订阅
                        @SuppressWarnings("unchecked")
                        Map<String, Object> subscriptionInfo = (Map<String, Object>) eventObject.get("subscription");
                        if (subscriptionInfo != null) {
                            handleSubscriptionData(order, subscriptionInfo, productInfo);
                        } else {
                            updateUserSubscription(order);
                        }
                        
                        return true;
                    } else {
                        logger.warn("通过requestId未找到订单: {}", requestId);
                    }
                }
                
                // 如果通过requestId未找到订单，则尝试通过checkoutId查找
                if (order == null && checkoutId != null && !checkoutId.isEmpty()) {
                    order = orderMapper.selectByOrderId(checkoutId);
                    
                    if (order != null) {
                        // 与上面相同的逻辑处理...
                        logger.info("通过checkoutId找到订单: {}", checkoutId);
                        
                        // 更新真实的订单ID
                        if (orderId != null) {
                            logger.info("更新订单ID: 从{}更新为{}", order.getOrderId(), orderId);
                            order.setOrderId(orderId);
                        }
                        
                        // 更新金额
                        if (amountObj != null) {
                            BigDecimal amount;
                            if (amountObj instanceof Integer) {
                                amount = new BigDecimal((Integer) amountObj);
                            } else if (amountObj instanceof Long) {
                                amount = new BigDecimal((Long) amountObj);
                            } else if (amountObj instanceof Double) {
                                amount = BigDecimal.valueOf((Double) amountObj);
                            } else {
                                amount = new BigDecimal(amountObj.toString());
                            }
                            order.setAmount(amount);
                        }
                        
                        // 更新订单状态为已支付
                        if ("paid".equals(status)) {
                            order.setStatus(1); // 已支付
                            order.setPayTime(new Date());
                        } else if ("failed".equals(status)) {
                            order.setStatus(2); // 支付失败
                        }
                        
                        order.setUpdateTime(new Date());
                        orderMapper.updateById(order);
                        
                        // 获取产品信息
                        @SuppressWarnings("unchecked")
                        Map<String, Object> productInfo = (Map<String, Object>) eventObject.get("product");

                        if (productInfo != null) {
                            String productName = (String) productInfo.get("name");
                            if (productName != null && !productName.isEmpty()) {
                                order.setProductName(productName);
                                orderMapper.updateById(order);
                            }
                        }
                        
                        // 如果是订阅商品，更新用户订阅
                        @SuppressWarnings("unchecked")
                        Map<String, Object> subscriptionInfo = (Map<String, Object>) eventObject.get("subscription");
                        if (subscriptionInfo != null) {
                            handleSubscriptionData(order, subscriptionInfo, productInfo);
                        } else {
                            updateUserSubscription(order);
                        }
                        
                        return true;
                    } else {
                        logger.warn("通过checkoutId未找到订单: {}", checkoutId);
                    }
                }
                
                logger.warn("未找到相关订单: requestId={}, checkoutId={}", requestId, checkoutId);
            }
            // 处理订阅相关事件
            else if ("subscription.active".equals(eventType) || 
                     "subscription.paid".equals(eventType) || 
                     "subscription.update".equals(eventType)) {
                // subscription事件，object是subscription对象本身
                String subscriptionId = (String) eventObject.get("id");
                String status = (String) eventObject.get("status");
                
                logger.info("订阅事件: subscriptionId={}, status={}", subscriptionId, status);
                
                // 获取订阅关联的订单
                // 如果webhook包含checkout信息，可以从中获取requestId
                @SuppressWarnings("unchecked")
                Map<String, Object> checkoutInfo = (Map<String, Object>) webhookData.get("checkout");
                String requestId = null;
                if (checkoutInfo != null) {
                    requestId = (String) checkoutInfo.get("request_id");
                }
                
                Order order = null;
                if (requestId != null && !requestId.isEmpty()) {
                    order = orderMapper.selectByRequestId(requestId);
                }
                
                // 如果找不到订单，尝试直接根据subscriptionId查询订阅
                Subscription subscription = subscriptionMapper.selectBySubscriptionId(subscriptionId);
                
                if (order != null || subscription != null) {
                    // 处理subscription.paid事件，更新用户积分
                    if ("subscription.paid".equals(eventType)) {
                        Map<String, Object> productInfo = (Map<String, Object>) eventObject.get("product");

                        // 获取产品ID
                        String productId = null;
                        String userId = null;
                        
                        if (productInfo != null) {
                            productId = (String) productInfo.get("id");
                        }
                        
                        // 确定用户ID
                        if (order != null) {
                            userId = String.valueOf(order.getUserId());
                        } else if (subscription != null) {
                            userId = subscription.getUserId();
                        }
                        
                        // 如果有产品ID和用户ID，则更新积分
                        if (productId != null && userId != null) {
                            try {
                                // 根据产品ID获取积分值
                                int pointsToAdd = com.aiconvert.common.ProductPointsEnum.getPointsByProductId(productId);
                                String productName = com.aiconvert.common.ProductPointsEnum.getPointsByProductName(productId);

                                logger.info("订阅支付事件，准备添加积分: userId={}, productId={}, points={}", 
                                    userId, productId, pointsToAdd);
                                
                                // 调用UserService更新积分
                                int newPoints = userService.updateUserPoints(userId, pointsToAdd, productName);
                                
                                logger.info("用户积分更新成功: userId={}, newPoints={}", userId, newPoints);
                            } catch (Exception e) {
                                logger.error("更新用户积分失败: userId={}, productId={}", userId, productId, e);
                            }
                        }
                    }
                    
                    // 获取产品信息
                    Map<String, Object> productInfo = (Map<String, Object>) eventObject.get("product");
                    handleSubscriptionData(order, eventObject, productInfo);
                    return true;
                } else {
                    logger.warn("未找到关联订单或订阅: subscriptionId={}, requestId={}", subscriptionId, requestId);
                }
            }
            // 处理订阅取消事件
            else if ("subscription.canceled".equals(eventType) || "subscription.expired".equals(eventType)) {
                String subscriptionId = (String) eventObject.get("id");
                
                // 更新订阅状态
                Subscription subscription = subscriptionMapper.selectBySubscriptionId(subscriptionId);
                if (subscription != null) {
                    subscription.setStatus("canceled");
                    subscription.setUpdateTime(new Date());
                    subscriptionMapper.updateById(subscription);
                    return true;
                } else {
                    logger.warn("未找到关联订阅: subscriptionId={}", subscriptionId);
                }
            }
            
            return false;
        } catch (Exception e) {
            logger.error("处理支付回调异常", e);
            return false;
        }
    }
    
    /**
     * 处理订阅数据
     */
    private void handleSubscriptionData(Order order, Map<String, Object> subscriptionData, Map<String, Object> productInfo) {
        String subscriptionId = (String) subscriptionData.get("id");
        String status = (String) subscriptionData.get("status");
        
        if (subscriptionId == null || subscriptionId.isEmpty()) {
            logger.warn("订阅ID为空");
            return;
        }
        
        // 获取用户ID
        String userId = order != null ? String.valueOf(order.getUserId()) : null;
        if (userId == null) {
            // 尝试从现有订阅中获取用户ID
            Subscription existingSubscription = subscriptionMapper.selectBySubscriptionId(subscriptionId);
            if (existingSubscription != null) {
                userId = existingSubscription.getUserId();
            } else {
                logger.warn("无法确定订阅所属用户");
                return;
            }
        }
        
        // 创建或更新订阅记录
        Subscription subscription = subscriptionMapper.selectBySubscriptionId(subscriptionId);
        
        if (subscription == null) {
            // 创建新订阅记录
            subscription = new Subscription();
            subscription.setUserId(userId);
            subscription.setSubscriptionId(subscriptionId);
            

            if (productInfo != null) {
                subscription.setPlanId((String) productInfo.get("id"));

                String productName = com.aiconvert.common.ProductPointsEnum.getPointsByProductName(subscription.getPlanId());

                subscription.setPlanName(productName);
            }
            
            subscription.setStatus(status);
            
            // 设置时间范围
            String currentPeriodStartDate = (String) subscriptionData.get("current_period_start_date");
            String currentPeriodEndDate = (String) subscriptionData.get("current_period_end_date");
            
            if (currentPeriodStartDate != null && currentPeriodEndDate != null) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    
                    subscription.setStartTime(dateFormat.parse(currentPeriodStartDate));
                    subscription.setEndTime(dateFormat.parse(currentPeriodEndDate));
                } catch (Exception e) {
                    logger.warn("解析订阅时间异常", e);
                    subscription.setStartTime(new Date());
                    // 默认30天订阅期
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_MONTH, 30);
                    subscription.setEndTime(calendar.getTime());
                }
            } else {
                subscription.setStartTime(new Date());
                // 默认30天订阅期
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, 30);
                subscription.setEndTime(calendar.getTime());
            }
            
            if (order != null) {
                subscription.setOrderId(order.getOrderId());
            }
            
            subscription.setCreateTime(new Date());
            subscription.setUpdateTime(new Date());
            
            subscriptionMapper.insert(subscription);
            
            logger.info("创建新订阅: subscriptionId={}, userId={}, status={}", 
                       subscriptionId, userId, status);
        } else {
            // 更新现有订阅
            subscription.setStatus(status);
            
            // 更新时间范围
            String currentPeriodStartDate = (String) subscriptionData.get("current_period_start_date");
            String currentPeriodEndDate = (String) subscriptionData.get("current_period_end_date");
            
            if (currentPeriodStartDate != null && currentPeriodEndDate != null) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    
                    subscription.setStartTime(dateFormat.parse(currentPeriodStartDate));
                    subscription.setEndTime(dateFormat.parse(currentPeriodEndDate));
                } catch (Exception e) {
                    logger.warn("解析订阅时间异常", e);
                }
            }
            
            subscription.setUpdateTime(new Date());
            subscriptionMapper.updateById(subscription);
            
            logger.info("更新现有订阅: subscriptionId={}, userId={}, status={}", 
                       subscriptionId, userId, status);
        }
    }
    
    /**
     * 更新用户订阅信息
     */
    private void updateUserSubscription(Order order) {
        // 检查是否是订阅产品(根据产品ID前缀判断)
        if (order.getProductId().startsWith("sub_")) {
            // 确定订阅期限(例如根据variantId判断)
            int months = 1; // 默认1个月
            
            if (order.getVariantId().contains("annual")) {
                months = 12;
            } else if (order.getVariantId().contains("quarterly")) {
                months = 3;
            }
            
            // 计算订阅结束时间
            Date startTime = new Date();
            Date endTime = new Date(startTime.getTime() + (long) months * 30 * 24 * 60 * 60 * 1000);
            
            // 生成订阅ID
            String subscriptionId = "sub_" + UUID.randomUUID().toString().replace("-", "");
            
            // 检查用户是否已有订阅
            Subscription existingSubscription = subscriptionMapper.selectByUserId(String.valueOf(order.getUserId()));
            
            if (existingSubscription != null) {
                // 更新现有订阅
                existingSubscription.setStatus("active");
                existingSubscription.setEndTime(endTime);
                existingSubscription.setUpdateTime(new Date());
                subscriptionMapper.updateById(existingSubscription);
            } else {
                // 创建新订阅
                Subscription subscription = new Subscription();
                subscription.setUserId(String.valueOf(order.getUserId()));
                subscription.setSubscriptionId(subscriptionId);
                subscription.setPlanId(order.getProductId());
                String productName = com.aiconvert.common.ProductPointsEnum.getPointsByProductName(subscription.getPlanId());
                subscription.setPlanName(productName);
                subscription.setPlanName(order.getProductName());
                subscription.setStatus("active");
                subscription.setStartTime(startTime);
                subscription.setEndTime(endTime);
                subscription.setOrderId(order.getOrderId());
                subscription.setCreateTime(new Date());
                subscription.setUpdateTime(new Date());
                
                subscriptionMapper.insert(subscription);
            }
            
            // 更新用户VIP状态或权限
            // 使用根据googleId查询用户的方法，或者其他可用的查询方法
            // 这里需要根据实际情况修改
            // User user = userMapper.selectById(order.getUserId());
            // if (user != null) {
            //     // 这里根据实际需求更新用户权限
            //     // 例如：user.setVipLevel(1);
            //     // userMapper.updateById(user);
            // }
        }
    }
} 