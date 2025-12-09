package com.nhnacademy.book_data_batch.batch.enrichment.context;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.EnrichmentFailureDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.EmbeddingFailureDto;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.EmbeddingSuccessDto;
import lombok.Getter;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Job-scoped 저장소로 Step 간 데이터 전달
 * ExecutionContext 대신 메모리에 저장하여 DB 크기 제약 회피
 */
@Getter
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class EnrichmentResultsHolder {
    
    private final ConcurrentLinkedQueue<EnrichmentSuccessDto> aladinSuccessResults = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<EnrichmentFailureDto> aladinFailedResults = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<EmbeddingSuccessDto> embeddingSuccessResults = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<EmbeddingFailureDto> embeddingFailureResults = new ConcurrentLinkedQueue<>();

    public void clearAll() {
        aladinSuccessResults.clear();
        aladinFailedResults.clear();
        embeddingSuccessResults.clear();
        embeddingFailureResults.clear();
    }
}
