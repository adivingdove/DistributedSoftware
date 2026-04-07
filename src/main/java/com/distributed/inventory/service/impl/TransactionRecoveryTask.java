package com.distributed.inventory.service.impl;

import com.distributed.inventory.entity.TccTransaction;
import com.distributed.inventory.entity.TransactionMessage;
import com.distributed.inventory.mapper.TccTransactionMapper;
import com.distributed.inventory.mapper.TransactionMessageMapper;
import com.distributed.inventory.service.TccOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionRecoveryTask {

    private static final Logger log = LoggerFactory.getLogger(TransactionRecoveryTask.class);

    @Autowired
    private TransactionMessageMapper transactionMessageMapper;

    @Autowired
    private TccTransactionMapper tccTransactionMapper;

    @Autowired
    private TccOrderService tccOrderService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 30000)
    public void retryPendingMessages() {
        List<TransactionMessage> pendingMessages = transactionMessageMapper.selectPendingMessages(5);
        for (TransactionMessage msg : pendingMessages) {
            try {
                kafkaTemplate.send(msg.getTopic(), msg.getMessageBody()).get();
                transactionMessageMapper.updateStatus(msg.getMessageId(), 1);
                log.info("[消息恢复] 重试发送成功 messageId={}, retryCount={}",
                        msg.getMessageId(), msg.getRetryCount() + 1);
            } catch (Exception e) {
                transactionMessageMapper.incrementRetryCount(msg.getMessageId());
                log.warn("[消息恢复] 重试发送失败 messageId={}: {}", msg.getMessageId(), e.getMessage());
            }
        }
        if (!pendingMessages.isEmpty()) {
            log.info("[消息恢复] 本轮处理 {} 条待发送消息", pendingMessages.size());
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void recoverHangingTccTransactions() {
        List<TccTransaction> hangingTx = tccTransactionMapper.selectHangingTransactions(5);
        for (TccTransaction tx : hangingTx) {
            try {
                tccOrderService.cancelPay(tx.getTxId());
                log.info("[TCC恢复] 悬挂事务已自动回滚 txId={}, orderId={}", tx.getTxId(), tx.getOrderId());
            } catch (Exception e) {
                log.warn("[TCC恢复] 回滚失败 txId={}: {}", tx.getTxId(), e.getMessage());
            }
        }
        if (!hangingTx.isEmpty()) {
            log.info("[TCC恢复] 本轮处理 {} 条悬挂事务", hangingTx.size());
        }
    }
}
