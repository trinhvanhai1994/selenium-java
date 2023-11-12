package com.datx.xwealth;

import com.datx.xwealth.entity.middleware.Members;
import com.datx.xwealth.entity.middleware.Roles;
import com.datx.xwealth.model.login.LoginResponse;
import com.datx.xwealth.repository.middleware.MiddlewareMemberRepository;
import com.datx.xwealth.repository.middleware.MiddlewareRoleRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class TestReportAgency extends BaseFunctionTest {
	private static final ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private MiddlewareRoleRepository middlewareRoleRepository;

	@Autowired
	private MiddlewareMemberRepository middlewareMemberRepository;

	@Before
	public void setup() {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

	@Test
	void reportAgency() {
		Roles agencyRole = middlewareRoleRepository.findFirstByRoleDefaultServices("agency");
		Roles partnerRole = middlewareRoleRepository.findFirstByRoleDefaultServices("partner");

		List<Members> agencyMembers = middlewareMemberRepository.findAllByRole(new ObjectId(agencyRole.id));
		List<Members> partnerMembers = middlewareMemberRepository.findAllByRole(new ObjectId(partnerRole.id));

		System.out.println(agencyMembers);
	}
}
