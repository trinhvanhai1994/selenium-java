package com.datx.xwealth.repository.middleware;

import com.datx.xwealth.entity.middleware.Roles;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MiddlewareRoleRepository extends MongoRepository<Roles, String> {
    Roles findFirstByRoleDefaultServices(String role);
}
