package com.datx.xwealth.entity.middleware;

import com.datx.xwealth.entity.BaseEntity;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Document("roles")
@Data
public class Roles extends BaseEntity {
    @Id
    public String id;

    public String name;

    @Field("display_name")
    public String displayName;

    public String description;
    public List<String> permissions;

    @Field("is_default")
    public boolean isDefault;

    @Field("is_insight")
    public boolean isInsight;

    @Field("is_issues_assign")
    public boolean is_issues_assign;

    @Field("role_default_services")
    public List<String> roleDefaultServices;
}
