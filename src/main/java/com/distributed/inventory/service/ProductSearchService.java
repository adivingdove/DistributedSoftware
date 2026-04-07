package com.distributed.inventory.service;

import com.distributed.inventory.entity.ProductDocument;

import java.util.List;

public interface ProductSearchService {

    void syncAll();

    void syncById(Long id);

    List<ProductDocument> search(String keyword);
}
