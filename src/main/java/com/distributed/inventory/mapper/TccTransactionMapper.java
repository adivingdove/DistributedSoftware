package com.distributed.inventory.mapper;

import com.distributed.inventory.entity.TccTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TccTransactionMapper {

    int insert(TccTransaction transaction);

    int updateStatus(@Param("txId") String txId, @Param("status") String status);

    TccTransaction selectByTxId(@Param("txId") String txId);

    List<TccTransaction> selectHangingTransactions(@Param("timeoutMinutes") Integer timeoutMinutes);

    TccTransaction selectTryingByOrderId(@Param("orderId") Long orderId);
}
