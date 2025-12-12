package com.nhnacademy.book_data_batch.batch.domain.aladin.processor;

import com.nhnacademy.book_data_batch.batch.core.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.batch.domain.aladin.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.domain.aladin.client.AladinQuotaTracker;
import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.AladinEnrichmentResult;
import com.nhnacademy.book_data_batch.batch.domain.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.domain.aladin.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BookBatchTarget DTO를 입력받아 Aladin API를 호출하고, 그 결과를 AladinEnrichmentResult로 반환하는 ItemProcessor.
 * API 호출 성공/실패 여부, 재시도 가능 여부 등을 AladinEnrichmentResult에 담아 다음 Step으로 전달한다.
 * AsyncItemProcessor가 이 Processor를 감싸서 비동기로 실행한다.
 * 쿼터 소진 시 Global Flag를 설정하고 이후 아이템들을 조용히 스킵한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AladinItemProcessor implements ItemProcessor<BookBatchTarget, AladinEnrichmentResult> {

    private final AladinApiClient aladinApiClient;
    private final AladinQuotaTracker aladinQuotaTracker;

    @Value("${aladin.api.keys}")
    private List<String> aladinApiKeys; // JobConfig에서 주입받음

    // 여러 API 키를 순환하며 사용하기 위한 인덱스
    private final AtomicInteger keyIndex = new AtomicInteger(0);

    @Override
    public AladinEnrichmentResult process(BookBatchTarget target) throws Exception {
        // 글로벌 쿼터 소진 플래그 확인 -> null(Skip)이 아닌 실패 결과 반환하여 Chunk를 빠르게 채워서 Writer로 보냄
        if (aladinQuotaTracker.isQuotaExhausted()) {
            return new AladinEnrichmentResult(target, null, false, "QUOTA_EXHAUSTED", true);
        }
        
        String isbn13 = target.isbn13();
        if (isbn13 == null || isbn13.isBlank()) {
            log.warn("[AladinItemProcessor] ISBN13이 비어 있습니다. Batch ID: {}", target.batchId());
            return new AladinEnrichmentResult(target, null, false, "ISBN13이 비어 있습니다.", false);
        }

        String apiKey = getNextApiKey();

        try {
            // 쿼터 확인 및 사용 시도 (Proactive Check)
            if (!aladinQuotaTracker.tryAcquire(apiKey)) {
                 log.warn("[AladinItemProcessor] API 키 {}의 쿼터가 소진되었습니다. Batch ID: {}. 이후 작업은 스킵됩니다.", apiKey, target.batchId());
                 aladinQuotaTracker.setQuotaExhausted(true); // Global Flag 설정
                 return new AladinEnrichmentResult(target, null, false, "QUOTA_EXHAUSTED", true);
            }

            Optional<AladinItemDto> aladinItemDto = aladinApiClient.lookupByIsbn(isbn13, apiKey);

            if (aladinItemDto.isPresent()) {
                log.debug("[AladinItemProcessor] ISBN {}에 대한 알라딘 데이터 찾음.", isbn13);
                return new AladinEnrichmentResult(target, aladinItemDto.get(), true, null, false);
            } else {
                log.debug("[AladinItemProcessor] ISBN {}에 대한 알라딘 데이터를 찾을 수 없습니다. 성공으로 처리합니다.", isbn13);
                return new AladinEnrichmentResult(target, null, true, "알라딘에서 찾을 수 없음", false);
            }

        } catch (RateLimitExceededException e) {
            // 알라딘 API 쿼터 초과 예외 (Reactive Check)
            log.warn("[AladinItemProcessor] 알라딘 API 쿼터 초과 - ISBN: {}, 메시지: {}. 이후 작업은 스킵됩니다.", isbn13, e.getMessage());
            aladinQuotaTracker.setQuotaExhausted(true); // Global Flag 설정
            return new AladinEnrichmentResult(target, null, false, "QUOTA_EXHAUSTED", true);

        } catch (RestClientException e) {
            // 네트워크 오류, 타임아웃 등 (재시도 가능) -> AOP 로깅 처리
            return new AladinEnrichmentResult(target, null, false, "네트워크/API 호출 오류: " + e.getMessage(), true);

        } catch (Exception e) {
            // 예측 불가능한 기타 오류 (코드 버그, 데이터 이상 등 -> 영구 실패) -> AOP 로깅 처리
            return new AladinEnrichmentResult(target, null, false, "알 수 없는 오류: " + e.getMessage(), false);
        }
    }

    /**
     * Round Robin 방식으로 다음 API 키를 가져옵니다.
     * 여러 스레드에서 동시에 호출될 수 있으므로 AtomicInteger를 사용합니다.
     */
    private String getNextApiKey() {
        if (aladinApiKeys == null || aladinApiKeys.isEmpty()) {
            throw new IllegalStateException("Aladin API 키 목록이 비어 있습니다.");
        }
        int currentIdx = keyIndex.getAndUpdate(operand -> (operand + 1) % aladinApiKeys.size());
        return aladinApiKeys.get(currentIdx);
    }
}
