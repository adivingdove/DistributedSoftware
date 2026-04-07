package com.distributed.inventory.service.impl;

import com.distributed.inventory.config.ReadOnly;
import com.distributed.inventory.entity.Product;
import com.distributed.inventory.entity.ProductDocument;
import com.distributed.inventory.mapper.ProductMapper;
import com.distributed.inventory.mapper.ProductSearchRepository;
import com.distributed.inventory.service.ProductSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchServiceImpl.class);

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Override
    @ReadOnly
    public void syncAll() {
        List<Product> products = productMapper.selectAll();
        List<ProductDocument> docs = products.stream()
                .map(ProductDocument::fromProduct)
                .collect(Collectors.toList());
        productSearchRepository.saveAll(docs);
        log.info("[ES同步] 同步了 {} 条商品数据到ElasticSearch", docs.size());
    }

    @Override
    @ReadOnly
    public void syncById(Long id) {
        Product product = productMapper.selectById(id);
        if (product != null) {
            productSearchRepository.save(ProductDocument.fromProduct(product));
            log.info("[ES同步] 同步商品ID={} 到ElasticSearch", id);
        }
    }

    @Override
    public List<ProductDocument> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            List<ProductDocument> result = new ArrayList<>();
            productSearchRepository.findAll().forEach(result::add);
            return result;
        }
        return productSearchRepository.findByNameContainingOrDescriptionContaining(keyword, keyword);
    }
}
