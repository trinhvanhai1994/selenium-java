package com.datx.xwealth;

import com.datx.xwealth.Utils.DateUtils;
import com.datx.xwealth.Utils.HttpUtils;
import com.datx.xwealth.model.login.LoginRequest;
import com.datx.xwealth.model.login.LoginResponse;
import com.datx.xwealth.model.login.RecommendBotResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

@SpringBootTest
class TestLogin {
	private static final String LOGIN_URL_UI = "https://dev-xwealth.datxasia.com/";
	private static final String LOGIN_URL_API = "https://dev-api.datxasia.com/api/auth/login";
	private static final String ROOT_GRAPHQL_API = "https://core.datx.vn/graphql";

	// File path json data
	private static final String PATH_LOGIN_SUCCESS = "templates/login/loginSuccess.json";
	private static final String PATH_LOGIN_FAIL = "templates/login/loginFail.json";

	private static final String PATH_RECOMMEND_BOT = "templates/login/getRecommendBot.json";
	private static final String PATH_LOGIN_EMAIL_INCORRECT = "templates/login/loginEmailIncorrect.json";
	private static final String PATH_LOGIN_PASSWORD_INCORRECT = "templates/login/loginPasswordIncorrect.json";

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
	void loginWebUI() {
		driver.get(LOGIN_URL_UI);
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(20));
		driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(20));
		driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20));

		WebElement username = driver.findElement(By.id("focus-input"));
		WebElement password = driver.findElement(By.id("password-input"));

		try {
			String body = readContentFileJson(PATH_LOGIN_SUCCESS);
			LoginRequest loginRequest = mapper.readValue(body, LoginRequest.class);

			username.sendKeys(loginRequest.getEmail());
			password.sendKeys(loginRequest.getPassword());

			WebElement submitButton = driver.findElement(By.id("login-button"));
			submitButton.click();

			WebElement submitNext = driver.findElement(By.className("gyDlZf"));
			submitNext.click();

			WebElement submitProduct = driver.findElement(By.className("fHCplw"));
			submitProduct.click();

			WebElement submitBuy = driver.findElement(By.className("iEObwT"));
			submitBuy.click();

			WebElement submitOrder = driver.findElement(By.className("bDxKUR"));
			submitOrder.click();

			driver.getWindowHandles().forEach(tab -> driver.switchTo().window(tab));

			WebElement submitPay = driver.findElement(By.className("gZxKoD"));
			submitPay.click();

			WebElement submitNCBPay = driver.findElement(By.id("NCB"));
			submitNCBPay.submit();

			WebElement enterCardNumber = driver.findElement(By.id("card_number_mask"));
			enterCardNumber.sendKeys("9704198526191432198");

			WebElement enterCardHolder = driver.findElement(By.id("cardHolder"));
			enterCardHolder.sendKeys("NGUYEN VAN A");

			WebElement enterCardDate = driver.findElement(By.id("cardDate"));
			enterCardDate.sendKeys("07/15");

			WebElement btnContinue = driver.findElement(By.id("btnContinue"));
			btnContinue.click();

		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void loginSuccess() {
		try {
			String body = readContentFileJson(PATH_LOGIN_SUCCESS);
			String responseBody = HttpUtils.getResponseApi(LOGIN_URL_API, body);
			LoginResponse loginResponse = mapper.readValue(responseBody, LoginResponse.class);
			if (Objects.isNull(loginResponse)) {
				Assertions.fail("Input data correct format! please");
			}

			LoginResponse.Data data = loginResponse.getData();
			if (data != null && !loginResponse.status) {
				Assertions.assertTrue(data.deviceLimitReached);
			} else {
				Assertions.assertTrue(loginResponse.isStatus());
			}

		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void loginFail() {
		try {
			String body = readContentFileJson(PATH_LOGIN_FAIL);
			String responseBody = HttpUtils.getResponseApi(LOGIN_URL_API, body);
			LoginResponse loginResponse = mapper.readValue(responseBody, LoginResponse.class);
			if (Objects.isNull(loginResponse)) {
				Assertions.fail("Input data correct format! please");
			}

			Assertions.assertFalse(loginResponse.status);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void loginEnterEmailIncorrect() {
		try {
			String body = readContentFileJson(PATH_LOGIN_EMAIL_INCORRECT);
			String responseBody = HttpUtils.getResponseApi(LOGIN_URL_API, body);
			LoginResponse loginResponse = mapper.readValue(responseBody, LoginResponse.class);
			if (Objects.isNull(loginResponse)) {
				Assertions.fail("Input data correct format! please");
			}

			int errorCodeUsername = 110;
			Assertions.assertEquals(errorCodeUsername, loginResponse.getErrorCode());
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void loginEnterPasswordIncorrect() {
		try {
			String body = readContentFileJson(PATH_LOGIN_PASSWORD_INCORRECT);
			String responseBody = HttpUtils.getResponseApi(LOGIN_URL_API, body);
			LoginResponse loginResponse = mapper.readValue(responseBody, LoginResponse.class);
			if (Objects.isNull(loginResponse)) {
				Assertions.fail("Input data correct format! please");
			}

			int errorCodeUsername = 199;
			Assertions.assertEquals(errorCodeUsername, loginResponse.getErrorCode());
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private String readContentFileJson(String path) {
		try {
			Resource companyDataResource = new ClassPathResource(path);
			File file = companyDataResource.getFile();
			return new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void checkClose() {
		try {
			String body = readContentFileJson(PATH_RECOMMEND_BOT);
			String responseBody = HttpUtils.getResponseApi(ROOT_GRAPHQL_API, body);
			RecommendBotResponse recommendBotResponse = mapper.readValue(responseBody, RecommendBotResponse.class);
			if (Objects.isNull(recommendBotResponse)) {
				Assertions.fail("Input data correct format! please");
			}

			RecommendBotResponse.Root data = recommendBotResponse.getData();
			ArrayList<RecommendBotResponse.RecommendationBot> recommendationBot = data.getRecommendationBot();
			recommendationBot.forEach(bot -> {
				String ticker = bot.getTicker();
				Date dateSell = DateUtils.convertStringToDate(bot.getDate());
				Date dateClose = DateUtils.addDate(-6);
				if (dateClose.after(dateSell) && !"close".equals(bot.getStatus())) {
					System.out.println("Error when not close" + ticker);
				}

				if (bot.getProfitPercent() > 10 && !"close".equals(bot.getStatus())) {
					System.out.println("Error when not close" + ticker);
				}
			});

		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@AfterAll
	static void exist() {
		driver.quit();
	}
}
