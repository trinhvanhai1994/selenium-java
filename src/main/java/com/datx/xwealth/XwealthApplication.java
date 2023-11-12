package com.datx.xwealth;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoClientFactoryBean;

@SpringBootApplication
public class XwealthApplication {

	public static void main(String[] args) {
		SpringApplication.run(XwealthApplication.class, args);
	}

//	@Bean
//	public MongoClientFactoryBean mongo(@Value("${spring.data.mongodb.uri}") String uri) throws Exception {
//		MongoClientFactoryBean mongo = new MongoClientFactoryBean();
//		ConnectionString conn = new ConnectionString(uri);
//		mongo.setConnectionString(conn);
//
//		MongoClient client = mongo.getObject();
//		client.listDatabaseNames()
//				.forEach(System.out::println);
//		return mongo;
//	}
}
