package com.datx.xwealth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"com.datx.xwealth.repository.sale"},
        mongoTemplateRef = SaleConfig.MONGO_TEMPLATE)
public class SaleConfig {
    protected static final String MONGO_TEMPLATE = "saleTemplate";
}
