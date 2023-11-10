package com.datx.xwealth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class Application {

	public static void main(String[] args) {
		SpringApplication.from(XwealthApplication::main).with(Application.class).run(args);
	}
}
