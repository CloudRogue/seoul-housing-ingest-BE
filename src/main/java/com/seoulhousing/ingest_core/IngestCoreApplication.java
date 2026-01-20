package com.seoulhousing.ingest_core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.seoulhousing.ingest_core")
public class IngestCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(IngestCoreApplication.class, args);
	}



}
