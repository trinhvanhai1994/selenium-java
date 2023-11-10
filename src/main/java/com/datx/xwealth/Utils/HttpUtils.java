package com.datx.xwealth.Utils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpUtils {

    public static String getResponseApiNonToken(String url, String body) {
        HttpResponse<JsonNode> response;
        try {
            response = Unirest.post(url)
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .asJson();
        } catch (UnirestException e) {
            log.error("*** getResponseApiNonToken error = {}", e.getMessage());
            throw new RuntimeException(e);
        }

        return response.getBody().toString();
    }

    public static String getResponseApiToken(String url, String body, String accessToken) {
        HttpResponse<JsonNode> response;
        try {
            response = Unirest.post(url)
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", accessToken)
                    .body(body)
                    .asJson();
        } catch (UnirestException e) {
            log.error("*** getResponseApiToken error = {}" + e.getMessage());
            throw new RuntimeException(e);
        }

        return response.getBody().toString();
    }
}
