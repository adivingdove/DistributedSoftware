package com.distributed.inventory.controller;

import com.distributed.inventory.common.PageResult;
import com.distributed.inventory.common.Result;
import com.distributed.inventory.entity.SeckillOrder;
import com.distributed.inventory.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @PostMapping("/{productId}")
    public Result<Map<String, Object>> seckill(
            @PathVariable Long productId,
            @RequestParam Long userId) {
        Long orderId = seckillService.seckill(userId, productId);
        Map<String, Object> data = new HashMap<>();
        data.put("orderId", String.valueOf(orderId));
        data.put("message", "秒杀请求已提交，订单处理中");
        return Result.success(data);
    }

    @GetMapping("/order/{orderId}")
    public Result<SeckillOrder> getOrder(@PathVariable Long orderId) {
        SeckillOrder order = seckillService.getOrderById(orderId);
        if (order == null) {
            return Result.error(404, "订单不存在或正在处理中");
        }
        return Result.success(order);
    }

    @GetMapping("/orders")
    public Result<PageResult<SeckillOrder>> getOrders(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return Result.success(seckillService.getOrdersByUserIdPaged(userId, page, size));
    }

    @PostMapping("/init-stock")
    public Result<Void> initStock() {
        seckillService.initStockToRedis();
        return Result.success();
    }

    @GetMapping("/sharding/order/{orderId}")
    public Result<SeckillOrder> getShardingOrder(@PathVariable Long orderId) {
        SeckillOrder order = seckillService.getShardingOrderById(orderId);
        if (order == null) {
            return Result.error(404, "分片库中订单不存在");
        }
        return Result.success(order);
    }

    @GetMapping("/sharding/orders")
    public Result<PageResult<SeckillOrder>> getShardingOrders(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size) {
        return Result.success(seckillService.getShardingOrdersByUserIdPaged(userId, page, size));
    }

    @PostMapping("/cancel")
    public Result<Void> cancelOrder(@RequestParam Long orderId, @RequestParam Long userId) {
        seckillService.cancelOrder(orderId, userId);
        return Result.success();
    }
}
