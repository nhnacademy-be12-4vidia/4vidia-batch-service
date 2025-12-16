package com.nhnacademy.book_data_batch.batch.domain.aladin.reader;

import com.nhnacademy.book_data_batch.batch.domain.aladin.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.api.AladinResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class AladinFetchReader implements ItemReader<AladinItemDto> {

    private final AladinApiClient aladinApiClient;
    private final com.nhnacademy.book_data_batch.batch.domain.aladin.client.AladinQuotaTracker aladinQuotaTracker;
    private final List<String> apiKeys;

    private int currentPage = 1;
    private int maxPage = Integer.MAX_VALUE;
    private final Queue<AladinItemDto> buffer = new LinkedList<>();
    private boolean isFirstCall = true;
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    @Override
    public AladinItemDto read() throws Exception {
        if (!buffer.isEmpty()) {
            return buffer.poll();
        }

        if (aladinQuotaTracker.isQuotaExhausted()) {
            log.warn("[AladinFetchReader] 쿼터 소진으로 작업을 중단합니다.");
            return null;
        }

        if (currentPage > maxPage) {
            log.info("[AladinFetchReader] 데이터 종료. Current: {}, Max: {}", currentPage, maxPage);
            return null;
        }

        String apiKey = getNextApiKey();

        if (!aladinQuotaTracker.tryAcquire(apiKey)) {
            log.warn("[AladinFetchReader] API 키 {}의 쿼터가 소진되었습니다. 작업을 중단합니다.", apiKey);
            aladinQuotaTracker.setQuotaExhausted(true);
            return null;
        }
        
        log.info("[AladinFetchReader] API 호출 시작 - Page: {}, Key: {}", currentPage, apiKey);
        Optional<AladinResponseDto> responseOpt = aladinApiClient.listItems(currentPage, apiKey);

        if (responseOpt.isEmpty()) {
            log.warn("[AladinFetchReader] API 응답이 없습니다. Page: {}", currentPage);
            return null;
        }

        AladinResponseDto response = responseOpt.get();
        List<AladinItemDto> items = response.item();

        if (items == null || items.isEmpty()) {
            log.info("[AladinFetchReader] 더 이상 데이터가 없습니다. Page: {}", currentPage);
            return null;
        }

        if (isFirstCall) {
            if (response.totalResults() != null && response.itemsPerPage() != null && response.itemsPerPage() > 0) {
                this.maxPage = (int) Math.ceil((double) response.totalResults() / response.itemsPerPage());
                log.info("[AladinFetchReader] 전체 결과 수: {}, 페이지 당 아이템: {}, 총 페이지 수: {}", 
                        response.totalResults(), response.itemsPerPage(), maxPage);
            }
            isFirstCall = false;
        }

        buffer.addAll(items);
        currentPage++;

        return buffer.poll();
    }

    private String getNextApiKey() {
        if (apiKeys == null || apiKeys.isEmpty()) {
            throw new IllegalStateException("Aladin API 키 목록이 비어 있습니다.");
        }
        int currentIdx = keyIndex.getAndUpdate(operand -> (operand + 1) % apiKeys.size());
        return apiKeys.get(currentIdx);
    }
}
