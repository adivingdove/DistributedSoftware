package com.distributed.inventory.mapper.order;

import com.distributed.inventory.entity.SeckillOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShardingSeckillOrderMapper {

    int insert(SeckillOrder order);

    SeckillOrder selectById(@Param("id") Long id);

    List<SeckillOrder> selectByUserId(@Param("userId") Long userId);

    List<SeckillOrder> selectByUserIdPaged(@Param("userId") Long userId, @Param("offset") int offset, @Param("size") int size);

    long countByUserId(@Param("userId") Long userId);

    SeckillOrder selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    int updateOrderStatus(@Param("id") Long id, @Param("status") Integer status);
}
