package com.distributed.inventory.service.impl;

import com.distributed.inventory.entity.TransactionMessage;
import com.distributed.inventory.mapper.TransactionMessageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReliableMessageService {

    private static final Logger log = LoggerFactory.getLogger(ReliableMessageService.class);

    @Autowired
    private TransactionMessageMapper transactionMessageMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public String saveAndSendMessage(String topic, String key, String messageBody) {
        String messageId = UUID.randomUUID().toString().replace("-", "");

        TransactionMessage msg = new TransactionMessage();
        msg.setMessageId(messageId);
        msg.setTopic(topic);
        msg.setMessageBody(messageBody);
        msg.setStatus(0);
        msg.setRetryCount(0);
        msg.setMaxRetry(5);
        transactionMessageMapper.insert(msg);
        log.info("[可靠消息] 消息已持久化 messageId={}, topic={}", messageId, topic);

        try {
            kafkaTemplate.send(topic, key, messageBody).get();
            transactionMessageMapper.updateStatus(messageId, 1);
            log.info("[可靠消息] 消息已发送 messageId={}", messageId);
        } catch (Exception e) {
            transactionMessageMapper.updateStatus(messageId, 3);
            log.warn("[可靠消息] 消息发送失败，等待重试 messageId={}: {}", messageId, e.getMessage());
        }

        return messageId;
    }

    public void confirmMessage(String messageId) {
        transactionMessageMapper.updateStatus(messageId, 2);
        log.info("[可靠消息] 消息已确认消费 messageId={}", messageId);
    }
}
