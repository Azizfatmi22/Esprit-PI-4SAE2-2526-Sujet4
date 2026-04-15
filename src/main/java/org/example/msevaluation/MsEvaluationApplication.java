package org.example.msevaluation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;



@SpringBootApplication
@EnableDiscoveryClient
public class MsEvaluationApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsEvaluationApplication.class, args);
    }

}
