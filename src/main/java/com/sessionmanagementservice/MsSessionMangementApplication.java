package com.sessionmanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsSessionMangementApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsSessionMangementApplication.class, args);
    }

}
