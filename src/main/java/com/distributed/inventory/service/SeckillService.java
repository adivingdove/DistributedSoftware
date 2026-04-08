package com.distributed.inventory.service;

import com.distributed.inventory.common.PageResult;
import com.distributed.inventory.entity.SeckillOrder;

import java.util.List;

public interface SeckillService {

    Long seckill(Long userId, Long productId);

    SeckillOrder getOrderById(Long orderId);

    List<SeckillOrder> getOrdersByUserId(Long userId);

    PageResult<SeckillOrder> getOrdersByUserIdPaged(Long userId, int page, int size);

    void initStockToRedis();

    SeckillOrder getShardingOrderById(Long orderId);

    List<SeckillOrder> getShardingOrdersByUserId(Long userId);

    PageResult<SeckillOrder> getShardingOrdersByUserIdPaged(Long userId, int page, int size);

    void cancelOrder(Long orderId, Long userId);
}
