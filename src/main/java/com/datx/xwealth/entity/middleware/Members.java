package com.datx.xwealth.entity.middleware;

import com.datx.xwealth.entity.BaseEntity;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@Document("members")
public class Members extends BaseEntity {
    @Id
    public String id;

    public String email;
    public String password;

    @Field("full_name")
    public String fullName;

    @Field("profile_photo")
    public String profilePhoto;

    public String address;

    @Field("country_code")
    public String countryCode;

    public String gender;
    public String dob;
    public String phone;
    public ObjectId role;

    @Field("last_login_time")
    public Date lastLoginTime;

    @Field("ref_code")
    public String refCode;

    @Field("sen_code")
    public String senCode;

    public Double money;
    public String currency;
    public String language;
    public String theme;

    @Field("auto_login")
    public boolean autoLogin;

    @Field("is_deleted")
    public boolean isDeleted;

    @Field("is_verified")
    public boolean isVerified;

    @Field("is_anchor_dat")
    public boolean isAnchorDat;

    public String status;
    public Double totalRevenue;
    public Double monthlyRevenue;

    @Field("customer_type")
    public String customerType;
}
