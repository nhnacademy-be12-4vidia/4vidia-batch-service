package com.nhnacademy.book_data_batch.batch.enrichment.aladin.client;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinResponseDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * Aladin ItemLookUp API 클라이언트
 * - ISBN13으로 도서 상세 정보 조회
 * - 실패 시 빈 Optional 반환 (예외 전파 안 함)
 * - API Rate Limit 대응을 위한 호출 간 딜레이 적용
 */
@Slf4j
@Component
public class AladinApiClient {

    private static final String BASE_URL = "http://www.aladin.co.kr/ttb/api/ItemLookUp.aspx";
    private static final String ITEM_ID_TYPE = "ISBN13";
    private static final String OUTPUT_FORMAT = "JS";
    private static final String VERSION = "20131101";
    
    private static final long API_CALL_DELAY_MS = 100;  // API 호출 간 100ms 딜레이
    private static final int MAX_RETRY_COUNT = 3;       // 최대 재시도 횟수
    private static final long RATE_LIMIT_WAIT_MS = 60_000;  // Rate Limit 시 대기 시간 (1분)

    private final RestTemplate restTemplate;

    public AladinApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * ISBN13으로 도서 상세 정보 조회 (재시도 로직 포함)
     *
     * @param isbn13 조회할 도서의 ISBN13
     * @param apiKey 사용할 Aladin API 키
     * @return 조회된 도서 정보 (없으면 빈 Optional)
     */
    public Optional<AladinItemDto> lookupByIsbn(String isbn13, String apiKey) {
        String url = buildUrl(isbn13, apiKey);
        
        int retryCount = 0;
        while (retryCount < MAX_RETRY_COUNT) {
            try {
                // API Rate Limit 대응 (호출 간 딜레이)
                Thread.sleep(API_CALL_DELAY_MS);
                
                AladinResponseDto response = restTemplate.getForObject(url, AladinResponseDto.class);

                if (response == null || response.item() == null || response.item().isEmpty()) {
                    log.debug("Aladin API 응답 없음: ISBN={}", isbn13);
                    return Optional.empty();
                }

                return Optional.of(response.item().get(0));

            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    retryCount++;
                    log.warn("Aladin API Rate Limit (429): ISBN={}, 재시도 {}/{}", 
                            isbn13, retryCount, MAX_RETRY_COUNT);
                    
                    if (retryCount >= MAX_RETRY_COUNT) {
                        log.error("Aladin API Rate Limit 재시도 횟수 초과: ISBN={}", isbn13);
                        throw new RateLimitExceededException(apiKey, 429, 
                                "Rate limit exceeded after " + MAX_RETRY_COUNT + " retries");
                    }
                    
                    // Rate Limit 시 1분 대기 후 재시도
                    waitForRateLimitReset();
                } else {
                    log.warn("Aladin API HTTP 오류: ISBN={}, status={}, error={}", 
                            isbn13, e.getStatusCode(), e.getMessage());
                    return Optional.empty();
                }
            } catch (RestClientException e) {
                log.warn("Aladin API 호출 실패: ISBN={}, error={}", isbn13, e.getMessage());
                return Optional.empty();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("API 호출 중단됨: ISBN={}", isbn13);
                return Optional.empty();
            }
        }
        
        return Optional.empty();
    }

    /**
     * Rate Limit 대기 (1분)
     */
    private void waitForRateLimitReset() {
        try {
            log.info("Rate Limit 대기 중... ({}초)", RATE_LIMIT_WAIT_MS / 1000);
            Thread.sleep(RATE_LIMIT_WAIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Rate Limit 대기 중 인터럽트 발생");
        }
    }

    private String buildUrl(String isbn13, String apiKey) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("ttbkey", apiKey)
                .queryParam("ItemIdType", ITEM_ID_TYPE)
                .queryParam("ItemId", isbn13)
                .queryParam("Output", OUTPUT_FORMAT)
                .queryParam("Version", VERSION)
                .build()
                .toUriString();
    }
}
