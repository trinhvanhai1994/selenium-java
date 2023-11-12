package com.datx.xwealth.repository.middleware;

import com.datx.xwealth.entity.middleware.Users;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MiddleUserRepository extends MongoRepository<Users, String> {
    boolean existsById(String id);
}
