package com.datx.xwealth.repository.sale;

import com.datx.xwealth.entity.sale.Users;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SaleUserRepository extends MongoRepository<Users, String> {
    boolean existsById(String id);
}
