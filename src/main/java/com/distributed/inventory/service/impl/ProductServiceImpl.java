package com.distributed.inventory.service.impl;

import com.distributed.inventory.config.ReadOnly;
import com.distributed.inventory.entity.Product;
import com.distributed.inventory.mapper.ProductMapper;
import com.distributed.inventory.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_CACHE_KEY = "product:detail:";
    private static final String CACHE_NULL_VALUE = "NULL";
    private static final long CACHE_TTL = 30;
    private static final long CACHE_NULL_TTL = 5;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @ReadOnly
    public Product getById(Long id) {
        String cacheKey = PRODUCT_CACHE_KEY + id;

        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);

        if (StringUtils.isNotBlank(cachedJson)) {
            if (CACHE_NULL_VALUE.equals(cachedJson)) {
                return null;
            }
            try {
                return objectMapper.readValue(cachedJson, Product.class);
            } catch (JsonProcessingException e) {
                stringRedisTemplate.delete(cacheKey);
            }
        }

        String lockKey = "lock:product:" + id;
        Product product;
        try {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(acquired)) {
                String doubleCheck = stringRedisTemplate.opsForValue().get(cacheKey);
                if (StringUtils.isNotBlank(doubleCheck)) {
                    stringRedisTemplate.delete(lockKey);
                    if (CACHE_NULL_VALUE.equals(doubleCheck)) {
                        return null;
                    }
                    try {
                        return objectMapper.readValue(doubleCheck, Product.class);
                    } catch (JsonProcessingException e) {
                        // ignore
                    }
                }

                product = productMapper.selectById(id);

                if (product == null) {
                    stringRedisTemplate.opsForValue().set(cacheKey, CACHE_NULL_VALUE,
                            CACHE_NULL_TTL, TimeUnit.MINUTES);
                } else {
                    long ttl = CACHE_TTL + (long) (Math.random() * 10);
                    try {
                        stringRedisTemplate.opsForValue().set(cacheKey,
                                objectMapper.writeValueAsString(product),
                                ttl, TimeUnit.MINUTES);
                    } catch (JsonProcessingException e) {
                        // ignore
                    }
                }
            } else {
                Thread.sleep(50);
                return getById(id);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            product = productMapper.selectById(id);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }

        return product;
    }

    @Override
    @ReadOnly
    public List<Product> listAll() {
        return productMapper.selectAll();
    }
}
