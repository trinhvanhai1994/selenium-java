package com.datx.xwealth.model;

import lombok.Data;

@Data
public class JwtTokenInfo {
    private UserInfo data;
    private int iat;
    private int exp;

    @Data
    public static class UserInfo {
        public String userid;
        public String source;
    }
}
