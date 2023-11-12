package com.datx.xwealth.entity.order;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("orders")
public class Orders {
    @Id
    public String id;
}
