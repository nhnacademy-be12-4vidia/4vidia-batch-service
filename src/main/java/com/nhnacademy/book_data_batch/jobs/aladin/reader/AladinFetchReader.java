package com.nhnacademy.book_data_batch.jobs.aladin.reader;

import com.nhnacademy.book_data_batch.infrastructure.client.aladin.AladinApiClient;
import com.nhnacademy.book_data_batch.infrastructure.client.aladin.AladinQuotaTracker;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.api.AladinResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class AladinFetchReader implements ItemReader<AladinItemDto>, ItemStream {

    private final AladinApiClient aladinApiClient;
    private final AladinQuotaTracker aladinQuotaTracker;
    private final List<String> apiKeys;

    private static final String CURRENT_PAGE_KEY = "aladin.fetch.currentPage";
    private static final String MAX_PAGE_KEY = "aladin.fetch.maxPage";
    private static final String IS_FIRST_CALL_KEY = "aladin.fetch.isFirstCall";
    private static final String BUFFER_INDEX_KEY = "aladin.fetch.bufferIndex";

    private int currentPage = 1;
    private int maxPage = Integer.MAX_VALUE;
    private final Queue<AladinItemDto> buffer = new LinkedList<>();
    private boolean isFirstCall = true;
    private final AtomicInteger keyIndex = new AtomicInteger(0);
    private int bufferIndex = 0; // 현재 페이지 내에서 읽은 아이템 인덱스

    @Override
    public AladinItemDto read() throws Exception {
        if (!buffer.isEmpty()) {
            bufferIndex++;
            return buffer.poll();
        }

        if (currentPage > maxPage) {
            log.info("[AladinFetchReader] 데이터 종료. Current: {}, Max: {}", currentPage, maxPage);
            return null;
        }

        String apiKey = getNextApiKey();

        if (!aladinQuotaTracker.tryAcquire(apiKey)) {
            log.warn("[AladinFetchReader] API 키 {}의 쿼터가 소진되었습니다.", apiKey);
            return null;
        }
        
        log.info("[AladinFetchReader] API 호출 시작 - Page: {}, Key: {}", currentPage, apiKey);
        Optional<AladinResponseDto> responseOpt = aladinApiClient.listItems(currentPage, apiKey);

        if (responseOpt.isEmpty()) {
            aladinQuotaTracker.releaseQuota(apiKey);
            log.warn("[AladinFetchReader] API 응답이 없습니다. Page: {}", currentPage);
            return null;
        }

        AladinResponseDto response = responseOpt.get();

        if (response.hasError()) {
            log.warn("[AladinFetchReader] API 에러 응답 - Page: {}, Code: {}, Msg: {}", 
                currentPage, response.errorCode(), response.errorMessage());
            return null;
        }

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

        // 재시작 시 이미 읽은 아이템 스킵 로직
        if (bufferIndex > 0 && bufferIndex < items.size()) {
            log.info("[AladinFetchReader] Skipping {} items in page {}", bufferIndex, currentPage);
            List<AladinItemDto> remainingItems = items.subList(bufferIndex, items.size());
            buffer.addAll(remainingItems);
        } else {
            buffer.addAll(items);
            bufferIndex = 0; // 새 페이지 로드 시 인덱스 초기화
        }

        currentPage++;
        bufferIndex++; // 현재 반환할 아이템 카운트
        return buffer.poll();
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        if (executionContext.containsKey(CURRENT_PAGE_KEY)) {
            this.currentPage = executionContext.getInt(CURRENT_PAGE_KEY);
            this.maxPage = executionContext.getInt(MAX_PAGE_KEY);
            this.isFirstCall = executionContext.getInt(IS_FIRST_CALL_KEY) == 1;
            this.bufferIndex = executionContext.getInt(BUFFER_INDEX_KEY);
            log.info("[AladinFetchReader] Restarting from page: {}, bufferIndex: {}", currentPage, bufferIndex);
        } else {
            log.info("[AladinFetchReader] Starting from scratch (Page 1)");
            this.bufferIndex = 0;
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // 버퍼에 남은 게 있다면 현재 페이지를 다시 읽어야 함 (currentPage는 이미 ++ 된 상태)
        int pageToSave = buffer.isEmpty() ? currentPage : currentPage - 1;
        int indexToSave = buffer.isEmpty() ? 0 : bufferIndex;

        executionContext.putInt(CURRENT_PAGE_KEY, pageToSave);
        executionContext.putInt(MAX_PAGE_KEY, maxPage);
        executionContext.putInt(IS_FIRST_CALL_KEY, isFirstCall ? 1 : 0);
        executionContext.putInt(BUFFER_INDEX_KEY, indexToSave);
    }

    private String getNextApiKey() {
        if (apiKeys == null || apiKeys.isEmpty()) {
            throw new IllegalStateException("Aladin API 키 목록이 비어 있습니다.");
        }
        int currentIdx = keyIndex.getAndUpdate(operand -> (operand + 1) % apiKeys.size());
        return apiKeys.get(currentIdx);
    }
}
