package com.company.officialwebsite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * OfficialWebsiteApplication：官网后台与 Portal 接口服务的 Spring Boot 启动入口。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class OfficialWebsiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(OfficialWebsiteApplication.class, args);
    }
}
