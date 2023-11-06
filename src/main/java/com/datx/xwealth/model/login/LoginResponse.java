package com.datx.xwealth.model.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class LoginResponse {
    @JsonProperty("error_string")
    public String errorMessage;
    @JsonProperty("error_code")
    public Integer errorCode;

    @JsonProperty("message_translated")
    public String messageTranslated;
    public Data data;
    public String message;
    public boolean status;

    @lombok.Data
    public static class Data {
        public boolean deviceLimitReached;
        public String shortAccessToken;
        @JsonProperty("access_token")
        public String accessToken;
        @JsonProperty("refresh_token")
        public String refreshToken;
        public Date lastActivityTimestamp;
        public String ipAddress;
        public String sessionToken;
        @JsonProperty("expired_time")
        public long expiredTime;
        public String source;
        public Date loginTimestamp;
        public Date expiresAt;
    }
}
