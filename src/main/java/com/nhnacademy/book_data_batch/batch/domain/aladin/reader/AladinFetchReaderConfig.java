package com.nhnacademy.book_data_batch.batch.domain.aladin.reader;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AladinFetchReaderConfig {

    private final EntityManagerFactory entityManagerFactory;

    @Value("${aladin.api.keys}")
    private List<String> aladinApiKeys;

    @Value("${aladin.api.quota-per-key}")
    private int quotaPerKey;

    @Value("${app.batch.chunk-size}")
    private int chunkSize;


}
