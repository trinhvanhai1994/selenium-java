package com.datx.xwealth.config;

import org.springframework.boot.autoconfigure.mongo.MongoConnectionDetails;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
public class MultipleMongoConfig {

    // Config for middleware database
    @Primary
    @Bean(name = "middleware")
    @ConfigurationProperties(prefix = "spring.data.mongodb.middleware")
    public MongoProperties getMiddlewareProps() {
        return new MongoProperties();
    }
    @Primary
    @Bean(name = "middlewareTemplate")
    public MongoTemplate middlewareTemplate() {
        return new MongoTemplate(middlewareFactory(getMiddlewareProps()));
    }
    @Primary
    @Bean
    public MongoDatabaseFactory middlewareFactory(MongoProperties properties) {
        return new SimpleMongoClientDatabaseFactory(properties.getUri());
    }

    // Config for sale-portal database
    @Bean(name = "sale")
    @ConfigurationProperties(prefix = "spring.data.mongodb.sale")
    public MongoProperties getSaleProps() {
        return new MongoProperties();
    }
    @Bean(name = "saleTemplate")
    public MongoTemplate saleTemplate() {
        return new MongoTemplate(saleFactory(getSaleProps()));
    }
    @Bean
    public MongoDatabaseFactory saleFactory(MongoProperties properties) {
        return new SimpleMongoClientDatabaseFactory(properties.getUri());
    }

    // Config for order-manager database
    @Bean(name = "order")
    @ConfigurationProperties(prefix = "spring.data.mongodb.order")
    public MongoProperties getOderProps() {
        return new MongoProperties();
    }
    @Bean(name = "orderTemplate")
    public MongoTemplate orderTemplate() {
        return new MongoTemplate(orderFactory(getOderProps()));
    }
    @Bean
    public MongoDatabaseFactory orderFactory(MongoProperties properties) {
        return new SimpleMongoClientDatabaseFactory(properties.getUri());
    }
}
