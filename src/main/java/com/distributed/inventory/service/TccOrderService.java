package com.distributed.inventory.service;

import java.util.Map;

public interface TccOrderService {

    Map<String, Object> tryPay(Long orderId, Long userId);

    void confirmPay(String txId);

    void cancelPay(String txId);
}
