package com.nhnacademy.book_data_batch.batch.aladin.writer;

import com.nhnacademy.book_data_batch.batch.aladin.dto.EnrichmentResultDto;
import com.nhnacademy.book_data_batch.batch.aladin.handler.EnrichmentSaveHandler;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Writer
 * -
 * - SaveHandler 목록을 순서대로 실행하여 저장 처리
 */
public class AladinEnrichmentWriter implements ItemWriter<EnrichmentResultDto> {

    private final List<EnrichmentSaveHandler> handlers;

    public AladinEnrichmentWriter(List<EnrichmentSaveHandler> handlers) {
        // order 기준 정렬하여 저장
        this.handlers = handlers.stream()
                .sorted(Comparator.comparingInt(EnrichmentSaveHandler::getOrder))
                .toList();
    }

    @Override
    public void write(Chunk<? extends EnrichmentResultDto> chunk) {

        List<EnrichmentResultDto> items = new ArrayList<>(chunk.getItems());

        if (items.isEmpty()) {
            return;
        }

        // 모든 Handler 순차 실행
        for (EnrichmentSaveHandler handler : handlers) {
            handler.handle(items);
        }
    }
}
