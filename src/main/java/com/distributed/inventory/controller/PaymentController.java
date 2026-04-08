package com.distributed.inventory.controller;

import com.distributed.inventory.common.Result;
import com.distributed.inventory.service.TccOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private TccOrderService tccOrderService;

    @PostMapping("/try")
    public Result<Map<String, Object>> tryPay(
            @RequestParam Long orderId,
            @RequestParam Long userId) {
        Map<String, Object> data = tccOrderService.tryPay(orderId, userId);
        return Result.success(data);
    }

    @PostMapping("/confirm")
    public Result<Void> confirmPay(@RequestParam String txId) {
        tccOrderService.confirmPay(txId);
        return Result.success();
    }

    @PostMapping("/cancel")
    public Result<Void> cancelPay(@RequestParam String txId) {
        tccOrderService.cancelPay(txId);
        return Result.success();
    }

    @GetMapping("/tx")
    public Result<String> getTxId(@RequestParam Long orderId) {
        String txId = tccOrderService.getTxIdByOrderId(orderId);
        if (txId == null) {
            return Result.error(404, "没有进行中的支付事务");
        }
        return Result.success(txId);
    }
}
