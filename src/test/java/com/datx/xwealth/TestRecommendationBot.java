package com.datx.xwealth;

import com.datx.xwealth.Utils.DateUtils;
import com.datx.xwealth.Utils.HttpUtils;
import com.datx.xwealth.Utils.JsonUtils;
import com.datx.xwealth.constant.PathConstant;
import com.datx.xwealth.constant.StringConstant;
import com.datx.xwealth.model.RecommendBot.RecommendBotResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@SpringBootTest
@Slf4j
public class TestRecommendationBot extends BaseFunctionTest {

    @Value("${xwealth.datx.api.root}")
    private String ROOT_GRAPHQL_API;

    private static WebDriver driver;
    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setup() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(false);
        options.addArguments("start-maximized"); // open Browser in maximized mode
        options.addArguments("disable-infobars"); // disabling infobars
        options.addArguments("--disable-extensions"); // disabling extensions
        options.addArguments("--disable-gpu"); // applicable to Windows os only
        options.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
        options.addArguments("--no-sandbox"); // Bypass OS security model
        options.addArguments("--disable-in-process-stack-traces");
        options.addArguments("--disable-logging");
        options.addArguments("--log-level=3");
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void testRecommendBot() {
        driver.quit();
        log.info("=============== Start test RecommendationBot ===============");
        int offset = 0;
        while (true) {
            List<RecommendBotResponse.RecommendationBot> recommendationBots = getListRecommendByPage(offset);

            log.info("*** listRecommendByPage offset " + offset);
            if (CollectionUtils.isEmpty(recommendationBots)) {
                break;
            }
            log.info("*** listRecommendByPage size " + recommendationBots.size());

            recommendationBots.forEach(bot -> {
                String ticker = bot.getTicker();
                Date dateSell = DateUtils.convertStringToDate(bot.getDate()); //Ngày bắn tín hiệu mua
                Date dateClose = DateUtils.addDate(-6); //Ngày bắn tín hiệu bán
                int maxPercent = 10;
                int minPercent = -8;

                boolean closeWithMaxPercent = bot.getProfitPercent() > maxPercent;
                boolean closeWithMinPercent = bot.getProfitPercent() < minPercent;
                boolean closeWithMoreThanSixDay = dateClose.after(dateSell); //Quá 6 ngày sau khi bắn tín hiệu mua

                // Check trường hợp báo đóng khi chưa đủ điều kiện
                boolean errorCloseWhenNotEnoughCondition = !closeWithMaxPercent && !closeWithMinPercent && !closeWithMoreThanSixDay && StringConstant.CLOSE_STATUS.equals(bot.getStatus());
                if (errorCloseWhenNotEnoughCondition) {
                    log.error("Error close ticker {} when do not enough condition!", ticker);
                }
                Assertions.assertFalse(errorCloseWhenNotEnoughCondition);

                // Check trường hợp không đóng khi quá 6 ngày sau khi bắn tín hiệu mua
                boolean errorOpenWhenMoreThanSixDays = closeWithMoreThanSixDay && StringConstant.OPEN_STATUS.equals(bot.getStatus());
                if (errorOpenWhenMoreThanSixDays) { //Nếu quá ngày bán mà vẫn chưa close sẽ báo lỗi
                    log.error("Error expire date when not close ticker {} ", ticker);
                }
                Assertions.assertFalse(errorOpenWhenMoreThanSixDays);

                // Check trường hợp không đóng khi lợi nhuận quá 10% sau khi bắn tín hiệu mua
                boolean errorOpenWhenMoreThanTenPercent = closeWithMaxPercent && StringConstant.OPEN_STATUS.equals(bot.getStatus());
                if (errorOpenWhenMoreThanTenPercent) { //Nếu lợi nhuận vượt quá 10% vẫn chưa close sẽ báo lỗi
                    log.info("Error Profit Percent > 10% when not close ticker {} ", ticker);
                }
                Assertions.assertFalse(errorOpenWhenMoreThanTenPercent);

                // Check trường hợp không đóng khi thua lỗ quá -8% sau khi bắn tín hiệu mua
                boolean errorOpenWhenLessThanEightPercent = closeWithMinPercent && StringConstant.OPEN_STATUS.equals(bot.getStatus());
                if (errorOpenWhenLessThanEightPercent) { //Nếu lỗ vượt quá -8% vẫn chưa close sẽ báo lỗi
                    log.info("Error Profit Percent < -8% when not close ticker {} ", ticker);
                }
                Assertions.assertFalse(errorOpenWhenLessThanEightPercent);
            });

            offset += recommendationBots.size();
        }

        log.info("=============== End test RecommendationBot ===============");
    }

    private List<RecommendBotResponse.RecommendationBot> getListRecommendByPage(int offset) {
        try {
            String bodyDefault = JsonUtils.readContentFileJson(PathConstant.PATH_RECOMMEND_BOT_JSON_FORMAT);

            String defaultOffset = "\"offset\":0";
            String newOffset = String.format("\"offset\":%s", offset);
            String body = bodyDefault.replaceAll(defaultOffset, newOffset);

            String responseBody = HttpUtils.getResponseApiToken(ROOT_GRAPHQL_API, body, getAccessToken());
            RecommendBotResponse recommendBotResponse = mapper.readValue(responseBody, RecommendBotResponse.class);
            if (Objects.isNull(recommendBotResponse) || Objects.isNull(recommendBotResponse.getData())) {
                log.error("*** getListRecommendByPage get response invalid!");
                return new ArrayList<>();
            }

            RecommendBotResponse.Root data = recommendBotResponse.getData();
            return data.getRecommendationBot();
        } catch (JsonProcessingException e) {
            log.error("getListRecommendByPage " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @AfterAll
    static void exist() {
        driver.quit();
    }
}
