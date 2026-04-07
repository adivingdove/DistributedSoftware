package com.distributed.inventory.controller;

import com.distributed.inventory.common.Result;
import com.distributed.inventory.config.DynamicDataSourceContextHolder;
import com.distributed.inventory.entity.Product;
import com.distributed.inventory.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Value("${server.port}")
    private String serverPort;

    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) {
            return Result.error(404, "商品不存在");
        }
        return Result.success(product);
    }

    @GetMapping("/list")
    public Result<List<Product>> list() {
        return Result.success(productService.listAll());
    }

    @GetMapping("/port")
    public Result<String> port() {
        return Result.success("Request handled by port: " + serverPort);
    }

    @GetMapping("/rw-test")
    public Result<Map<String, Object>> rwTest() {
        Map<String, Object> result = new HashMap<>();
        DynamicDataSourceContextHolder.setDataSourceType(DynamicDataSourceContextHolder.MASTER);
        List<Product> writeResult = productService.listAll();
        result.put("writeDataSource", "master");
        result.put("writeCount", writeResult.size());

        List<Product> readResult = productService.listAll();
        result.put("readDataSource", "slave (@ReadOnly)");
        result.put("readCount", readResult.size());

        result.put("message", "查看后端日志确认: [读写分离] 切换到从库");
        return Result.success(result);
    }
}
