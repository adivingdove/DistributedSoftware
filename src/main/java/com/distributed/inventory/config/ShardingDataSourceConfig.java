package com.distributed.inventory.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
@MapperScan(basePackages = "com.distributed.inventory.mapper.order",
        sqlSessionFactoryRef = "shardingSqlSessionFactory")
public class ShardingDataSourceConfig {

    @Value("${spring.datasource.shard0.url:jdbc:mysql://localhost:3309/seckill_order_0?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true}")
    private String shard0Url;
    @Value("${spring.datasource.shard0.username:root}")
    private String shard0Username;
    @Value("${spring.datasource.shard0.password:minatoaqua3710}")
    private String shard0Password;

    @Value("${spring.datasource.shard1.url:jdbc:mysql://localhost:3310/seckill_order_1?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true}")
    private String shard1Url;
    @Value("${spring.datasource.shard1.username:root}")
    private String shard1Username;
    @Value("${spring.datasource.shard1.password:minatoaqua3710}")
    private String shard1Password;

    private DruidDataSource createDruidDataSource(String url, String username, String password) throws SQLException {
        DruidDataSource ds = new DruidDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setInitialSize(5);
        ds.setMinIdle(5);
        ds.setMaxActive(20);
        ds.setMaxWait(60000);
        ds.init();
        return ds;
    }

    @Bean("shardingDataSource")
    public DataSource shardingDataSource() throws Exception {
        Map<String, DataSource> dataSourceMap = new HashMap<>();
        dataSourceMap.put("ds0", createDruidDataSource(shard0Url, shard0Username, shard0Password));
        dataSourceMap.put("ds1", createDruidDataSource(shard1Url, shard1Username, shard1Password));

        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();

        ShardingTableRuleConfiguration orderTableRule = new ShardingTableRuleConfiguration(
                "seckill_order", "ds$->{0..1}.seckill_order_$->{0..1}");
        orderTableRule.setDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration("user_id", "db-inline"));
        orderTableRule.setTableShardingStrategy(
                new StandardShardingStrategyConfiguration("id", "table-inline"));
        shardingRuleConfig.getTables().add(orderTableRule);

        Properties dbAlgoProps = new Properties();
        dbAlgoProps.setProperty("algorithm-expression", "ds$->{user_id % 2}");
        shardingRuleConfig.getShardingAlgorithms().put("db-inline",
                new AlgorithmConfiguration("INLINE", dbAlgoProps));

        Properties tableAlgoProps = new Properties();
        tableAlgoProps.setProperty("algorithm-expression", "seckill_order_$->{id % 2}");
        shardingRuleConfig.getShardingAlgorithms().put("table-inline",
                new AlgorithmConfiguration("INLINE", tableAlgoProps));

        Properties ssProps = new Properties();
        ssProps.setProperty("sql-show", "true");

        return ShardingSphereDataSourceFactory.createDataSource(
                dataSourceMap, Collections.singletonList(shardingRuleConfig), ssProps);
    }

    @Bean("shardingSqlSessionFactory")
    public SqlSessionFactory shardingSqlSessionFactory() throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(shardingDataSource());
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath:mapper/order/*.xml"));
        factoryBean.setTypeAliasesPackage("com.distributed.inventory.entity");
        org.apache.ibatis.session.Configuration config = new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        config.setLogImpl(org.apache.ibatis.logging.stdout.StdOutImpl.class);
        factoryBean.setConfiguration(config);
        return factoryBean.getObject();
    }

    @Bean("shardingSqlSessionTemplate")
    public SqlSessionTemplate shardingSqlSessionTemplate() throws Exception {
        return new SqlSessionTemplate(shardingSqlSessionFactory());
    }
}
