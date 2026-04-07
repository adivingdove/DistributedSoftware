package com.distributed.inventory.entity;

import java.io.Serializable;
import java.util.Date;

public class TccTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String txId;
    private Long orderId;
    private Long userId;
    private String status;
    private String txType;
    private Date createTime;
    private Date updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTxId() { return txId; }
    public void setTxId(String txId) { this.txId = txId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTxType() { return txType; }
    public void setTxType(String txType) { this.txType = txType; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}
