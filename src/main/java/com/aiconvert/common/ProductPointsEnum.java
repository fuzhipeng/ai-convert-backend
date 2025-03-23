package com.aiconvert.common;

/**
 * 产品积分映射枚举
 * 用于定义不同产品ID对应的积分奖励
 */
public enum ProductPointsEnum {
    // 定义产品ID和对应的积分值
    BASIC_MONTHLY("prod_72KZNhyOAGYixU89pUMenL", 900, "基础月度订阅"),
    PRO_MONTHLY("prod_2JByiTnhcB99sEdTUsbvDQ", 2000, "专业月度订阅"),

    ;

    private final String productId;
    private final int points;
    private final String description;

    ProductPointsEnum(String productId, int points, String description) {
        this.productId = productId;
        this.points = points;
        this.description = description;
    }

    public String getProductId() {
        return productId;
    }

    public int getPoints() {
        return points;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据产品ID查找对应的积分值
     * @param productId 产品ID
     * @return 对应的积分值，如果找不到对应的产品则返回默认值50
     */
    public static int getPointsByProductId(String productId) {
        if (productId == null || productId.isEmpty()) {
            return 50; // 默认积分
        }
        
        for (ProductPointsEnum product : ProductPointsEnum.values()) {
            if (product.getProductId().equals(productId)) {
                return product.getPoints();
            }
        }
        
        return 50; // 默认积分
    }

      /**
     * 根据产品ID查找对应的积分值
     * @param productId 产品ID
     * @return 对应的积分值，如果找不到对应的产品则返回默认值50
     */
    public static String getPointsByProductName(String productId) {
        if (productId == null || productId.isEmpty()) {
            return "基础月度订阅"; // 默认积分
        }
        
        for (ProductPointsEnum product : ProductPointsEnum.values()) {
            if (product.getProductId().equals(productId)) {
                return product.getDescription();
            }
        }
        
        return "基础月度订阅"; // 默认积分
    }
} 