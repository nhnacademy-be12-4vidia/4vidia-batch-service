package com.nhnacademy.book_data_batch.batch.domain.aladin.reader;

import com.nhnacademy.book_data_batch.batch.domain.aladin.client.AladinApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AladinFetchReaderConfig {

    private final AladinApiClient aladinApiClient;
    private final com.nhnacademy.book_data_batch.batch.domain.aladin.client.AladinQuotaTracker aladinQuotaTracker;

    @Value("${aladin.api.keys}")
    private List<String> aladinApiKeys;

    @Bean
    public AladinFetchReader aladinFetchReader() {
        return new AladinFetchReader(aladinApiClient, aladinQuotaTracker, aladinApiKeys);
    }
}
