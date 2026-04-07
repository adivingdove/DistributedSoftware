package com.distributed.inventory.service;

import com.distributed.inventory.entity.Product;

import java.util.List;

public interface ProductService {

    Product getById(Long id);

    List<Product> listAll();
}
