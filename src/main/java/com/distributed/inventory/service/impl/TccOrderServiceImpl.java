package com.distributed.inventory.service.impl;

import com.distributed.inventory.entity.SeckillOrder;
import com.distributed.inventory.entity.TccTransaction;
import com.distributed.inventory.mapper.SeckillOrderMapper;
import com.distributed.inventory.mapper.TccTransactionMapper;
import com.distributed.inventory.service.TccOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TccOrderServiceImpl implements TccOrderService {

    private static final Logger log = LoggerFactory.getLogger(TccOrderServiceImpl.class);

    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    @Autowired
    private TccTransactionMapper tccTransactionMapper;

    @Override
    @Transactional
    public Map<String, Object> tryPay(Long orderId, Long userId) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态异常，当前状态: " + order.getStatus());
        }
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        String txId = UUID.randomUUID().toString().replace("-", "");

        seckillOrderMapper.updateOrderStatus(orderId, 3);
        log.info("[TCC-Try] 订单已冻结 orderId={}, txId={}", orderId, txId);

        TccTransaction tx = new TccTransaction();
        tx.setTxId(txId);
        tx.setOrderId(orderId);
        tx.setUserId(userId);
        tx.setStatus("TRYING");
        tx.setTxType("ORDER_PAY");
        tccTransactionMapper.insert(tx);

        Map<String, Object> result = new HashMap<>();
        result.put("txId", txId);
        result.put("orderId", String.valueOf(orderId));
        result.put("message", "支付预处理成功，请确认支付");
        return result;
    }

    @Override
    @Transactional
    public void confirmPay(String txId) {
        TccTransaction tx = tccTransactionMapper.selectByTxId(txId);
        if (tx == null) {
            throw new RuntimeException("事务不存在: " + txId);
        }
        if ("CONFIRMED".equals(tx.getStatus())) {
            log.warn("[TCC-Confirm] 事务已确认，幂等跳过 txId={}", txId);
            return;
        }
        if (!"TRYING".equals(tx.getStatus())) {
            throw new RuntimeException("事务状态异常，无法确认: " + tx.getStatus());
        }

        seckillOrderMapper.updateOrderStatus(tx.getOrderId(), 1);
        log.info("[TCC-Confirm] 订单支付成功 orderId={}", tx.getOrderId());

        tccTransactionMapper.updateStatus(txId, "CONFIRMED");
        log.info("[TCC-Confirm] 事务已确认 txId={}", txId);
    }

    @Override
    @Transactional
    public void cancelPay(String txId) {
        TccTransaction tx = tccTransactionMapper.selectByTxId(txId);
        if (tx == null) {
            throw new RuntimeException("事务不存在: " + txId);
        }
        if ("CANCELLED".equals(tx.getStatus())) {
            log.warn("[TCC-Cancel] 事务已取消，幂等跳过 txId={}", txId);
            return;
        }
        if (!"TRYING".equals(tx.getStatus())) {
            throw new RuntimeException("事务状态异常，无法取消: " + tx.getStatus());
        }

        seckillOrderMapper.updateOrderStatus(tx.getOrderId(), 0);
        log.info("[TCC-Cancel] 订单已恢复待支付 orderId={}", tx.getOrderId());

        tccTransactionMapper.updateStatus(txId, "CANCELLED");
        log.info("[TCC-Cancel] 事务已取消 txId={}", txId);
    }
}
