package com.distributed.inventory.service.impl;

import com.distributed.inventory.dto.SeckillMessage;
import com.distributed.inventory.entity.Product;
import com.distributed.inventory.entity.SeckillOrder;
import com.distributed.inventory.mapper.ProductMapper;
import com.distributed.inventory.mapper.SeckillOrderMapper;
import com.distributed.inventory.mapper.order.ShardingSeckillOrderMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeckillOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(SeckillOrderConsumer.class);

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private ShardingSeckillOrderMapper shardingSeckillOrderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReliableMessageService reliableMessageService;

    @KafkaListener(topics = "seckill-order", groupId = "seckill-group")
    @Transactional
    public void handleSeckillOrder(String message) {
        try {
            SeckillMessage msg = objectMapper.readValue(message, SeckillMessage.class);
            log.info("[Kafka消费] 收到秒杀消息 userId={}, productId={}, orderId={}",
                    msg.getUserId(), msg.getProductId(), msg.getOrderId());

            SeckillOrder existing = seckillOrderMapper.selectByUserIdAndProductId(
                    msg.getUserId(), msg.getProductId());
            if (existing != null) {
                log.warn("[Kafka消费] 重复订单，跳过 userId={}, productId={}", msg.getUserId(), msg.getProductId());
                if (msg.getMessageId() != null) {
                    reliableMessageService.confirmMessage(msg.getMessageId());
                }
                return;
            }

            int affected = seckillOrderMapper.deductStock(msg.getProductId());
            if (affected == 0) {
                log.warn("[Kafka消费] 库存不足，扣减失败 productId={}", msg.getProductId());
                return;
            }

            Product product = productMapper.selectById(msg.getProductId());
            SeckillOrder order = new SeckillOrder();
            order.setId(msg.getOrderId());
            order.setUserId(msg.getUserId());
            order.setProductId(msg.getProductId());
            order.setProductName(product != null ? product.getName() : "");
            order.setPrice(product != null ? product.getPrice() : null);
            order.setStatus(0);
            seckillOrderMapper.insert(order);

            try {
                shardingSeckillOrderMapper.insert(order);
                log.info("[Kafka消费] 订单已写入分片库 orderId={}, userId={}", msg.getOrderId(), msg.getUserId());
            } catch (Exception e) {
                log.warn("[Kafka消费] 分片库写入失败（不影响主流程）: {}", e.getMessage());
            }

            if (msg.getMessageId() != null) {
                reliableMessageService.confirmMessage(msg.getMessageId());
            }

            log.info("[Kafka消费] 订单创建成功（下单+扣库存事务一致） orderId={}", msg.getOrderId());
        } catch (Exception e) {
            log.error("[Kafka消费] 处理失败: {}", e.getMessage(), e);
        }
    }
}
