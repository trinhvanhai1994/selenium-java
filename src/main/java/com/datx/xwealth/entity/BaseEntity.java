package com.datx.xwealth.entity;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
public class BaseEntity {
    @Field("createdAt")
    public Date created_at;

    @Field("updatedAt")
    public Date updated_at;
}
