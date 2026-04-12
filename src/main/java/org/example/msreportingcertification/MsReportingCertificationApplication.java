package org.example.msreportingcertification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MsReportingCertificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsReportingCertificationApplication.class, args);
	}

}
