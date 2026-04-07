package com.distributed.inventory.mapper;

import com.distributed.inventory.entity.TransactionMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TransactionMessageMapper {

    int insert(TransactionMessage message);

    int updateStatus(@Param("messageId") String messageId, @Param("status") Integer status);

    int incrementRetryCount(@Param("messageId") String messageId);

    List<TransactionMessage> selectPendingMessages(@Param("maxRetry") Integer maxRetry);

    TransactionMessage selectByMessageId(@Param("messageId") String messageId);
}
