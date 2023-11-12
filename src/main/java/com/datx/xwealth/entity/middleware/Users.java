package com.datx.xwealth.entity.middleware;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public class Users {
    @Id
    public String id;
}
