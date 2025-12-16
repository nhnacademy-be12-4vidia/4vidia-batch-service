package com.nhnacademy.book_data_batch.batch.domain.aladin.step;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Step;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class AladinFetchStepConfig {

    private static final String ALADIN_FETCH_STEP_NAME = "aladinFetchStep";

    @Bean
    public Step aladinFetchStep() {
        return null;
    }
}
