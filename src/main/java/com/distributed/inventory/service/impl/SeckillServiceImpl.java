package com.distributed.inventory.service.impl;

import com.distributed.inventory.config.ReadOnly;
import com.distributed.inventory.dto.SeckillMessage;
import com.distributed.inventory.entity.Product;
import com.distributed.inventory.entity.SeckillOrder;
import com.distributed.inventory.mapper.ProductMapper;
import com.distributed.inventory.mapper.SeckillOrderMapper;
import com.distributed.inventory.mapper.order.ShardingSeckillOrderMapper;
import com.distributed.inventory.service.SeckillService;
import com.distributed.inventory.util.SnowflakeIdGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class SeckillServiceImpl implements SeckillService {

    private static final Logger log = LoggerFactory.getLogger(SeckillServiceImpl.class);

    private static final String STOCK_KEY = "seckill:stock:";
    private static final String ORDER_FLAG_KEY = "seckill:order:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ReliableMessageService reliableMessageService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private ShardingSeckillOrderMapper shardingSeckillOrderMapper;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        initStockToRedis();
    }

    @Override
    public void initStockToRedis() {
        List<Product> products = productMapper.selectAll();
        for (Product product : products) {
            stringRedisTemplate.opsForValue().set(
                    STOCK_KEY + product.getId(),
                    String.valueOf(product.getStock()));
        }
        log.info("[秒杀初始化] 已加载 {} 个商品库存到Redis", products.size());
    }

    @Override
    public Long seckill(Long userId, Long productId) {
        String orderFlag = ORDER_FLAG_KEY + userId + ":" + productId;
        Boolean alreadyOrdered = stringRedisTemplate.opsForValue().setIfAbsent(orderFlag, "1");
        if (Boolean.FALSE.equals(alreadyOrdered)) {
            throw new RuntimeException("您已经参与过该商品的秒杀");
        }

        String stockKey = STOCK_KEY + productId;
        Long stock = stringRedisTemplate.opsForValue().decrement(stockKey);
        if (stock == null || stock < 0) {
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.delete(orderFlag);
            throw new RuntimeException("商品已售罄");
        }

        Long orderId = snowflakeIdGenerator.nextId();
        SeckillMessage message = new SeckillMessage(userId, productId, orderId);
        try {
            String json = objectMapper.writeValueAsString(message);
            String messageId = reliableMessageService.saveAndSendMessage(
                    "seckill-order", String.valueOf(productId), json);
            log.info("[秒杀] 可靠消息已发送 userId={}, productId={}, orderId={}, messageId={}",
                    userId, productId, orderId, messageId);
        } catch (JsonProcessingException e) {
            stringRedisTemplate.opsForValue().increment(stockKey);
            stringRedisTemplate.delete(orderFlag);
            throw new RuntimeException("秒杀消息发送失败");
        }

        return orderId;
    }

    @Override
    @ReadOnly
    public SeckillOrder getOrderById(Long orderId) {
        return seckillOrderMapper.selectById(orderId);
    }

    @Override
    @ReadOnly
    public List<SeckillOrder> getOrdersByUserId(Long userId) {
        return seckillOrderMapper.selectByUserId(userId);
    }

    @Override
    public SeckillOrder getShardingOrderById(Long orderId) {
        return shardingSeckillOrderMapper.selectById(orderId);
    }

    @Override
    public List<SeckillOrder> getShardingOrdersByUserId(Long userId) {
        return shardingSeckillOrderMapper.selectByUserId(userId);
    }
}
