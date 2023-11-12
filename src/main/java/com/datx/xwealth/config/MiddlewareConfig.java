package com.datx.xwealth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = {"com.datx.xwealth.repository.middleware"},
        mongoTemplateRef = MiddlewareConfig.MONGO_TEMPLATE)
public class MiddlewareConfig {
    protected static final String MONGO_TEMPLATE = "middlewareTemplate";
}
