package com.distributed.inventory.controller;

import com.distributed.inventory.common.Result;
import com.distributed.inventory.entity.ProductDocument;
import com.distributed.inventory.service.ProductSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private ProductSearchService productSearchService;

    @GetMapping
    public Result<List<ProductDocument>> search(@RequestParam(required = false, defaultValue = "") String keyword) {
        List<ProductDocument> results = productSearchService.search(keyword);
        return Result.success(results);
    }

    @PostMapping("/sync")
    public Result<Void> syncAll() {
        productSearchService.syncAll();
        return Result.success();
    }
}
