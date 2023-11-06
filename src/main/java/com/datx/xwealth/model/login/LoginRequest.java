package com.datx.xwealth.model.login;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    private Device device;

    @Data
    public static class Device {
        private String name;
        private String platform;

        @JsonProperty("device_token")
        private String deviceToken;
        @JsonProperty("device_id_app")
        private String deviceIdApp;
        @JsonProperty("fcm_token")
        private Object fcmToken;
        @JsonProperty("meta_data")
        private String metaData;
    }
}


