package com.datx.xwealth.repository.middleware;

import com.datx.xwealth.entity.middleware.Members;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MiddlewareMemberRepository extends MongoRepository<Members, String> {
    List<Members> findAllByRole(ObjectId roleId);
}
