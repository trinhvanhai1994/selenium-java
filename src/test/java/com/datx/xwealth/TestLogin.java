package com.datx.xwealth;

import com.datx.xwealth.Utils.JsonUtils;
import com.datx.xwealth.constant.PathConstant;
import com.datx.xwealth.model.login.LoginRequest;
import com.datx.xwealth.model.login.LoginResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

@SpringBootTest
class TestLogin extends BaseFunctionTest {

	@Value("${xwealth.datx.url.root}")
	private String LOGIN_URL_UI;

	private static WebDriver driver;
	private static final ObjectMapper mapper = new ObjectMapper();


	@BeforeAll
	static void setup() {
		WebDriverManager.chromedriver().setup();
		ChromeOptions options = new ChromeOptions();
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
			String body = JsonUtils.readContentFileJson(PathConstant.PATH_LOGIN_SUCCESS);
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
	void loginApiSuccess() {
		LoginResponse loginResponse = getLoginInfo();
		LoginResponse.Data data = loginResponse.getData();
		if (data != null && !loginResponse.status) {
			Assertions.assertTrue(data.deviceLimitReached);
		} else {
			Assertions.assertTrue(loginResponse.isStatus());
		}
	}
}
