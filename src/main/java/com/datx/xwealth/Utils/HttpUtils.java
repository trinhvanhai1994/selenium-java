package com.datx.xwealth.Utils;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpUtils {
    public static String getResponseApi(String url, String body) {
        HttpResponse<JsonNode> response = null;
        try {
            response = Unirest.post(url)
                    .header("accept", "application/json")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .asJson();
        } catch (UnirestException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        return response.getBody().toString();
    }
}
