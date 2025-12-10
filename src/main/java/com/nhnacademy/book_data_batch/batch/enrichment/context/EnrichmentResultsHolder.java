package com.nhnacademy.book_data_batch.batch.enrichment.context;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.EnrichmentFailureDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.EmbeddingFailureDto;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.EmbeddingSuccessDto;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.BookEmbeddingTarget;
import lombok.Getter;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

@Getter
@Component
@JobScope
public class EnrichmentResultsHolder {
    
    private final ConcurrentLinkedQueue<EnrichmentSuccessDto> aladinSuccessResults = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<EnrichmentFailureDto> aladinFailedResults = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<EmbeddingSuccessDto> embeddingSuccessResults = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<EmbeddingFailureDto> embeddingFailureResults = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<BookEmbeddingTarget> embeddingTargets = new ConcurrentLinkedQueue<>();

    public void clearAll() {
        aladinSuccessResults.clear();
        aladinFailedResults.clear();
        embeddingSuccessResults.clear();
        embeddingFailureResults.clear();
        embeddingTargets.clear();
    }
}
