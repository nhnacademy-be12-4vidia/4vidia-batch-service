package com.nhnacademy.book_data_batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableBatchProcessing
public class DataParserApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataParserApplication.class, args);
    }

}
