package com.distributed.inventory.config;

import com.distributed.inventory.util.SnowflakeIdGenerator;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeckillConfig {

    @Bean
    public SnowflakeIdGenerator snowflakeIdGenerator(
            @Value("${seckill.snowflake.worker-id:1}") long workerId,
            @Value("${seckill.snowflake.datacenter-id:1}") long datacenterId) {
        return new SnowflakeIdGenerator(workerId, datacenterId);
    }

    @Bean
    public NewTopic seckillTopic() {
        return new NewTopic("seckill-order", 3, (short) 1);
    }
}
