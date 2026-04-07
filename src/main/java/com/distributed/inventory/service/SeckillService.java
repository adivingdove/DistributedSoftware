package com.distributed.inventory.service;

import com.distributed.inventory.entity.SeckillOrder;

import java.util.List;

public interface SeckillService {

    Long seckill(Long userId, Long productId);

    SeckillOrder getOrderById(Long orderId);

    List<SeckillOrder> getOrdersByUserId(Long userId);

    void initStockToRedis();
}
