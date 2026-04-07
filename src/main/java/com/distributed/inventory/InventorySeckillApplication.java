package com.distributed.inventory;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan(basePackages = "com.distributed.inventory.mapper",
        sqlSessionFactoryRef = "primarySqlSessionFactory")
public class InventorySeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventorySeckillApplication.class, args);
    }
}
