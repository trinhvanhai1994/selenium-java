package com.datx.xwealth;

import com.datx.xwealth.Utils.HttpUtils;
import com.datx.xwealth.model.login.LoginRequest;
import com.datx.xwealth.model.login.LoginResponse;
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
import java.util.Objects;

@SpringBootTest
class TestLogin {
	private static final String LOGIN_URL_UI = "https://dev-xwealth.datxasia.com/";
	private static final String LOGIN_URL_API = "https://dev-api.datxasia.com/api/auth/login";

	// File path json data
	private static final String PATH_LOGIN_SUCCESS = "templates/login/loginSuccess.json";
	private static final String PATH_LOGIN_FAIL = "templates/login/loginFail.json";
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
		driver.getTitle();
		driver.manage().timeouts().implicitlyWait(Duration.ofMillis(500));

		WebElement username = driver.findElement(By.id("focus-input"));
		WebElement password = driver.findElement(By.id("password-input"));

		try {
			String body = readContentFileJson(PATH_LOGIN_SUCCESS);
			LoginRequest loginRequest = mapper.readValue(body, LoginRequest.class);

			username.sendKeys(loginRequest.getEmail());
			password.sendKeys(loginRequest.getPassword());

			WebElement submitButton = driver.findElement(By.id("login-button"));
			submitButton.click();

			Thread.sleep(5000);
			WebElement submitNext = driver.findElement(By.className("gyDlZf"));
			submitNext.click();

			Thread.sleep(5000);
			WebElement submitProduct = driver.findElement(By.className("fHCplw"));
			submitProduct.click();

			Thread.sleep(5000);
			WebElement submitBuy = driver.findElement(By.className("iEObwT"));
			submitBuy.click();

			Thread.sleep(5000);
			WebElement submitOrder = driver.findElement(By.className("bDxKUR"));
			submitOrder.click();

			driver.getWindowHandles().forEach(tab -> driver.switchTo().window(tab));

			Thread.sleep(8000);
			WebElement submitPay = driver.findElement(By.className("gZxKoD"));
			submitPay.click();

			Thread.sleep(8000);
			WebElement submitNCBPay = driver.findElement(By.id("NCB"));
			submitNCBPay.click();

			WebElement enterCardNumber = driver.findElement(By.id("card_number_mask"));
			enterCardNumber.sendKeys("9704198526191432198");

			WebElement enterCardHolder = driver.findElement(By.id("cardHolder"));
			enterCardHolder.sendKeys("NGUYEN VAN A");

			WebElement enterCardDate = driver.findElement(By.id("cardDate"));
			enterCardDate.sendKeys("07/15");

			WebElement btnContinue = driver.findElement(By.id("btnContinue"));
			btnContinue.click();

		} catch (JsonProcessingException | InterruptedException e) {
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

	@AfterAll
	static void exist() {
//		driver.quit();
	}
}
