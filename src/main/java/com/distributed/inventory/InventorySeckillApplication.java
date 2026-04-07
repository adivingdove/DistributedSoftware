package com.distributed.inventory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.distributed.inventory.mapper")
public class InventorySeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventorySeckillApplication.class, args);
    }
}
