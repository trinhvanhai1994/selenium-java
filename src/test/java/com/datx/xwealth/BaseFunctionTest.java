package com.datx.xwealth;

import com.datx.xwealth.Utils.HttpUtils;
import com.datx.xwealth.Utils.JsonUtils;
import com.datx.xwealth.constant.PathConstant;
import com.datx.xwealth.constant.StringConstant;
import com.datx.xwealth.model.JwtTokenInfo;
import com.datx.xwealth.model.login.LoginResponse;
import com.datx.xwealth.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Objects;

@Slf4j
public class BaseFunctionTest {

    @Value("${xwealth.datx.api.login}")
    private String LOGIN_API;

    @Value("${xwealth.datx.account.email}")
    private String ACCOUNT_EMAIL;

    @Value("${xwealth.datx.account.password}")
    private String ACCOUNT_PASSWORD;

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private UserService userService;

    @Before
    public void setup() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String getAccessToken() {
        try {
            String body = JsonUtils.readContentFileJson(PathConstant.PATH_LOGIN_JSON_FORMAT);
            String updateEmail = body.replaceAll(StringConstant.ACCOUNT_EMAIL_KEY, ACCOUNT_EMAIL);
            String updatePassword = updateEmail.replaceAll(StringConstant.ACCOUNT_PASSWORD_KEY, ACCOUNT_PASSWORD);

            String responseBody = HttpUtils.getResponseApiNonToken(LOGIN_API, updatePassword);
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

            this.checkAccessTokenValid(accessToken);

            return accessToken;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public LoginResponse getLoginInfo() {
        try {
            String body = JsonUtils.readContentFileJson(PathConstant.PATH_LOGIN_JSON_FORMAT);
            String updateEmail = body.replaceAll(StringConstant.ACCOUNT_EMAIL_KEY, ACCOUNT_EMAIL);
            String updatePassword = updateEmail.replaceAll(StringConstant.ACCOUNT_PASSWORD_KEY, ACCOUNT_PASSWORD);

            String responseBody = HttpUtils.getResponseApiNonToken(LOGIN_API, updatePassword);
            LoginResponse loginResponse = mapper.readValue(responseBody, LoginResponse.class);
            if (Objects.isNull(loginResponse) || Objects.isNull(loginResponse.data)) {
                Assertions.fail("Login information incorrect! Please check again");
            }

            this.checkAccessTokenValid(loginResponse.data.shortAccessToken);

            return loginResponse;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkAccessTokenValid(String accessToken) {
        JwtTokenInfo jwtTokenInfo = jwtToObject(accessToken);
        if (!userService.checkMemberIdExits(jwtTokenInfo.getData().getUserid())) {
            Assertions.fail("do not exits MemberId = " + jwtTokenInfo.getData().getUserid());
        }
    }

    public JwtTokenInfo jwtToObject(String accessToken) {
        String[] splitJwtContent = accessToken.split("\\.");
        String base64EncodedBody = splitJwtContent[1];

        Base64 base64Url = new Base64(true);
        String body = new String(base64Url.decode(base64EncodedBody));
        try {
            return mapper.readValue(body, JwtTokenInfo.class);
        } catch (JsonProcessingException e) {
            log.error("jwtToObject error {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
