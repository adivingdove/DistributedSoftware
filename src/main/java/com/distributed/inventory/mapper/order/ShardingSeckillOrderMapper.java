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

    SeckillOrder selectByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);
}
