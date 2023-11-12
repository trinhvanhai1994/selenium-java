package com.datx.xwealth.repository.order;

import com.datx.xwealth.entity.order.Orders;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderOrderRepository extends MongoRepository<Orders, String> {
    boolean existsById(String id);
}
