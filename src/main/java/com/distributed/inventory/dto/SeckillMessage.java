package com.distributed.inventory.dto;

import java.io.Serializable;

public class SeckillMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long productId;
    private Long orderId;

    public SeckillMessage() {}

    public SeckillMessage(Long userId, Long productId, Long orderId) {
        this.userId = userId;
        this.productId = productId;
        this.orderId = orderId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
}
