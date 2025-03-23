package com.aiconvert.entity;

import java.util.Date;

public class UserPoints {
    private Long id;
    private Long userId;
    private Integer points;
    private Integer pointsChange;  // 积分变动值
    private String remark;         // 备注，说明积分变动原因
    private Date createTime;
    private Date updateTime;
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Integer getPoints() {
        return points;
    }
    
    public void setPoints(Integer points) {
        this.points = points;
    }
    
    public Integer getPointsChange() {
        return pointsChange;
    }
    
    public void setPointsChange(Integer pointsChange) {
        this.pointsChange = pointsChange;
    }
    
    public String getRemark() {
        return remark;
    }
    
    public void setRemark(String remark) {
        this.remark = remark;
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