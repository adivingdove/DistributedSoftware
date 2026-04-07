package com.distributed.inventory.mapper;

import com.distributed.inventory.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    Product selectById(@Param("id") Long id);

    List<Product> selectAll();

    int insert(Product product);

    int updateStock(@Param("id") Long id, @Param("stock") Integer stock);
}
