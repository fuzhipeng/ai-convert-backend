package com.aiconvert.entity;

import java.math.BigDecimal;
import java.util.Date;

public class Order {
    private Long id;
    private String userId;
    private String orderId;        // 订单ID(系统生成)
    private String requestId;      // 请求ID(用于关联Creem请求)
    private String productId;      // 产品ID
    private String variantId;      // 产品变体ID
    private String productName;    // 产品名称
    private BigDecimal amount;     // 支付金额
    private Integer status;        // 支付状态: 0-未支付 1-已支付 2-支付失败 3-已取消
    private String checkoutUrl;    // 结账URL
    private String paymentMethod;  // 支付方式
    private String metadata;       // 元数据(JSON格式)
    private Date payTime;          // 支付时间
    private Date expireTime;       // 订阅过期时间(如果是订阅商品)
    private Date createTime;       // 创建时间
    private Date updateTime;       // 更新时间
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getVariantId() {
        return variantId;
    }
    
    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public void setProductName(String productName) {
        this.productName = productName;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public String getCheckoutUrl() {
        return checkoutUrl;
    }
    
    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public Date getPayTime() {
        return payTime;
    }
    
    public void setPayTime(Date payTime) {
        this.payTime = payTime;
    }
    
    public Date getExpireTime() {
        return expireTime;
    }
    
    public void setExpireTime(Date expireTime) {
        this.expireTime = expireTime;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    public Date getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
} 