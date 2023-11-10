package com.datx.xwealth;

import com.datx.xwealth.Utils.HttpUtils;
import com.datx.xwealth.Utils.JsonUtils;
import com.datx.xwealth.constant.PathConstant;
import com.datx.xwealth.model.login.LoginResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Value;

import java.util.Objects;

public class BaseFunctionTest {

    @Value("${xwealth.datx.api.login}")
    private String LOGIN_API;

    private static final ObjectMapper mapper = new ObjectMapper();

    public String getAccessToken() {
        try {
            String body = JsonUtils.readContentFileJson(PathConstant.PATH_LOGIN_SUCCESS);
            String responseBody = HttpUtils.getResponseApiNonToken(LOGIN_API, body);
            LoginResponse loginResponse = mapper.readValue(responseBody, LoginResponse.class);
            if (Objects.isNull(loginResponse) || Objects.isNull(loginResponse.data)) {
                Assertions.fail("Login information incorrect! Please check again");
            }

            String accessToken;
            LoginResponse.Data data = loginResponse.getData();
            if (!loginResponse.status && data.deviceLimitReached) {
                accessToken = data.getShortAccessToken();
            } else {
                accessToken = data.getAccessToken();
            }
            return accessToken;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public LoginResponse getLoginInfo() {
        try {
            String body = JsonUtils.readContentFileJson(PathConstant.PATH_LOGIN_SUCCESS);
            String responseBody = HttpUtils.getResponseApiNonToken(LOGIN_API, body);
            LoginResponse loginResponse = mapper.readValue(responseBody, LoginResponse.class);
            if (Objects.isNull(loginResponse) || Objects.isNull(loginResponse.data)) {
                Assertions.fail("Login information incorrect! Please check again");
            }
            return loginResponse;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
