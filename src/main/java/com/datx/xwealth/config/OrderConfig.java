package com.datx.xwealth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"com.datx.xwealth.repository.order"},
        mongoTemplateRef = OrderConfig.MONGO_TEMPLATE)
public class OrderConfig {
    protected static final String MONGO_TEMPLATE = "orderTemplate";
}
