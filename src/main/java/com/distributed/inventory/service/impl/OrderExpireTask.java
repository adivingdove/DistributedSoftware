package com.distributed.inventory.service.impl;

import com.distributed.inventory.entity.SeckillOrder;
import com.distributed.inventory.mapper.SeckillOrderMapper;
import com.distributed.inventory.mapper.order.ShardingSeckillOrderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OrderExpireTask {

    private static final Logger log = LoggerFactory.getLogger(OrderExpireTask.class);

    private static final String STOCK_KEY = "seckill:stock:";
    private static final String ORDER_FLAG_KEY = "seckill:order:";

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private ShardingSeckillOrderMapper shardingSeckillOrderMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Scheduled(fixedDelay = 15000)
    public void cancelExpiredOrders() {
        List<SeckillOrder> expiredOrders = seckillOrderMapper.selectExpiredOrders(50);
        for (SeckillOrder order : expiredOrders) {
            try {
                doCancel(order);
                log.info("[订单过期] 自动取消 orderId={}, productId={}", order.getId(), order.getProductId());
            } catch (Exception e) {
                log.warn("[订单过期] 取消失败 orderId={}: {}", order.getId(), e.getMessage());
            }
        }
        if (!expiredOrders.isEmpty()) {
            log.info("[订单过期] 本轮处理 {} 条超时订单", expiredOrders.size());
        }
    }

    @Transactional
    public void doCancel(SeckillOrder order) {
        seckillOrderMapper.updateOrderStatus(order.getId(), 2);
        seckillOrderMapper.restoreStock(order.getProductId());
        try {
            shardingSeckillOrderMapper.updateOrderStatus(order.getId(), 2);
        } catch (Exception e) {
            log.warn("[订单过期] 分片库状态同步失败: {}", e.getMessage());
        }

        String stockKey = STOCK_KEY + order.getProductId();
        stringRedisTemplate.opsForValue().increment(stockKey);
        String orderFlag = ORDER_FLAG_KEY + order.getUserId() + ":" + order.getProductId();
        stringRedisTemplate.delete(orderFlag);
    }
}
