package com.datx.xwealth.model.login;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;

@Data
public class RecommendBotResponse {
    private Root data;

    @Data
    public static class Root {
        private ArrayList<RecommendationBot> recommendationBot;
    }

    @Data
    public static class RecommendationBot {
        private String date;
        private double latestPrice;
        private String ticker;
        private double tpPrice;
        private int quantity;
        private Object maxAllocation;
        private double openPrice;
        private double closedPrice;
        private double profitPercent;
        private Date closedDate;
        private String exchange;
        private String id;
        private Date openDate;
        private double profit;
        private String sector;
        private String status;
    }
}
