package com.nhnacademy.book_data_batch.jobs.aladin.processor;

import com.nhnacademy.book_data_batch.jobs.aladin.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.infrastructure.client.aladin.AladinApiClient;
import com.nhnacademy.book_data_batch.infrastructure.client.aladin.AladinQuotaTracker;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.AladinEnrichmentResult;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.jobs.aladin.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class AladinItemProcessor implements ItemProcessor<BookBatchTarget, AladinEnrichmentResult> {

    private static final String QUOTA_EXHAUSTED_FLAG = "QUOTA_EXHAUSTED";

    private final AladinApiClient aladinApiClient;
    private final AladinQuotaTracker aladinQuotaTracker;

    @Value("${aladin.api.keys}")
    private List<String> aladinApiKeys; // JobConfig에서 주입받음

    // 여러 API 키를 순환하며 사용하기 위한 인덱스
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    @Override
    public AladinEnrichmentResult process(BookBatchTarget target) throws Exception {
        if (aladinQuotaTracker.isAllKeysExhausted(aladinApiKeys)) {
            return new AladinEnrichmentResult(target, null, false, "모든 API 키 할당량 소진", true);
        }
        
        String isbn13 = target.isbn13();
        if (isbn13 == null || isbn13.isBlank()) {
            log.warn("[AladinItemProcessor] ISBN13이 비어 있습니다. Batch ID: {}", target.batchId());
            return new AladinEnrichmentResult(target, null, false, "ISBN13이 비어 있습니다.", false);
        }

        String apiKey = getNextApiKey();

        if (!aladinQuotaTracker.tryAcquire(apiKey)) {
            return null;
        }

        try {
            Optional<AladinItemDto> aladinItemDto = aladinApiClient.lookupByIsbn(isbn13, apiKey);

            if (aladinItemDto.isPresent()) {
                log.debug("[AladinItemProcessor] ISBN {}에 대한 알라딘 데이터 찾음.", isbn13);
                return new AladinEnrichmentResult(target, aladinItemDto.get(), true, null, false);
            } else {
                log.debug("[AladinItemProcessor] ISBN {}에 대한 알라딘 데이터를 찾을 수 없습니다. 성공으로 처리합니다.", isbn13);
                return new AladinEnrichmentResult(target, null, true, "알라딘에서 찾을 수 없음", false);
            }

        } catch (RateLimitExceededException e) {
            aladinQuotaTracker.releaseQuota(apiKey);
            log.warn("[AladinItemProcessor] 알라딘 API 쿼터 초과 - ISBN: {}, 메시지: {}", isbn13, e.getMessage());
            return new AladinEnrichmentResult(target, null, false, "쿼터 초과", true);

        } catch (RestClientException e) {
            aladinQuotaTracker.releaseQuota(apiKey);
            log.warn("[AladinItemProcessor] 네트워크/API 호출 오류 - ISBN: {}, 메시지: {}", isbn13, e.getMessage());
            return new AladinEnrichmentResult(target, null, false, "네트워크/API 호출 오류: " + e.getMessage(), true);

        } catch (Exception e) {
            aladinQuotaTracker.releaseQuota(apiKey);
            log.error("[AladinItemProcessor] 알 수 없는 오류 - ISBN: {}", isbn13, e);
            return new AladinEnrichmentResult(target, null, false, "알 수 없는 오류: " + e.getMessage(), false);
        }
    }

    private String getNextApiKey() {
        if (aladinApiKeys == null || aladinApiKeys.isEmpty()) {
            throw new IllegalStateException("Aladin API 키 목록이 비어 있습니다.");
        }
        int currentIdx = keyIndex.getAndUpdate(operand -> (operand + 1) % aladinApiKeys.size());
        return aladinApiKeys.get(currentIdx);
    }
}
