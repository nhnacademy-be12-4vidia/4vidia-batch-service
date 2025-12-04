package com.nhnacademy.book_data_batch.batch.enrichment.aladin.client;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinResponseDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * Aladin ItemLookUp API 클라이언트
 * - ISBN13으로 도서 상세 정보 조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AladinApiClient {

    private static final String BASE_URL = "http://www.aladin.co.kr/ttb/api/ItemLookUp.aspx";
    private static final String ITEM_ID_TYPE = "ISBN13";
    private static final String OUTPUT_FORMAT = "JS";
    private static final String VERSION = "20131101";

    // API 호출 설정
    private static final long API_CALL_DELAY_MS = 100;       // 호출 간 딜레이 (100ms)
    
    // 429 Rate Limit 재시도 설정
    private static final int MAX_RETRY_429 = 3;              // 최대 재시도 횟수
    private static final long RATE_LIMIT_WAIT_MS = 60_000;   // 대기 시간 (1분)
    
    // 네트워크 오류 재시도 설정
    private static final int MAX_RETRY_NETWORK = 1;          // 최대 재시도 횟수
    private static final long NETWORK_RETRY_WAIT_MS = 1_000; // 대기 시간 (1초)

    private final RestClient restClient;

    /**
     * ISBN13으로 도서 상세 정보 조회 (재시도 로직 포함)
     *
     * @param isbn13 조회할 도서의 ISBN13
     * @param apiKey 사용할 Aladin API 키
     * @return 조회된 도서 정보 (없으면 빈 Optional)
     */
    public Optional<AladinItemDto> lookupByIsbn(String isbn13, String apiKey) {
        String url = buildUrl(isbn13, apiKey);

        int retry429Count = 0;
        int retryNetworkCount = 0;
        boolean firstTry = true;

        while (retry429Count <= MAX_RETRY_429) {
            try {
                // API Rate Limit 대응 (호출 간 딜레이)
                if (!firstTry) {
                    Thread.sleep(API_CALL_DELAY_MS);
                } else {
                    firstTry = false;
                }

                AladinResponseDto response = restClient
                        .get()
                        .uri(url)
                        .retrieve()
                        .body(AladinResponseDto.class);

                if (response == null || response.item() == null || response.item().isEmpty()) {
                    log.debug("[Aladin API] 응답 없음: ISBN={}", isbn13);
                    return Optional.empty();
                }

                return Optional.of(response.item().get(0));

                // 재시도1: 429 Rate Limit - 1분 대기 후 최대 3회 재시도
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    retry429Count++;
                    if (retry429Count > MAX_RETRY_429) {
                        log.warn("[Aladin API] 429 Rate Limit 재시도 초과: ISBN={}", isbn13);
                        throw new RateLimitExceededException(apiKey, 429, 
                                "[Aladin API] Rate limit 초과:  " + MAX_RETRY_429 + "번 재시도 실패");
                    }
                    log.warn("[Aladin API] 429 Rate Limit: ISBN={}, 재시도 {}/{}", isbn13, retry429Count, MAX_RETRY_429);
                    sleep(RATE_LIMIT_WAIT_MS);
                } else {
                    log.debug("[Aladin API] HTTP 오류: ISBN={}, status={}", isbn13, e.getStatusCode());
                    return Optional.empty();
                }

                // 재시도2: 네트워크 오류 처리 - 1초 대기 후 1회 재시도
            } catch (RestClientException e) {
                if (retryNetworkCount < MAX_RETRY_NETWORK) {
                    retryNetworkCount++;
                    log.debug("[Aladin API] 네트워크 오류, 재시도 {}/{}: ISBN={}", retryNetworkCount, MAX_RETRY_NETWORK, isbn13);
                    sleep(NETWORK_RETRY_WAIT_MS);
                    continue;
                }
                log.warn("[Aladin API] 호출 실패: ISBN={}, error={}", isbn13, e.getMessage());
                return Optional.empty();

                // 재시도3: 인터럽트 처리 - 즉시 종료
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[Aladin API] 호출 중단됨: ISBN={}", isbn13);
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * 지정된 시간 동안 대기
     */
    private void sleep(long ms) {
        try {
            if (ms >= 1000) {
                log.info("[Aladin API] 대기 중... ({}초)", ms / 1000);
            }
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[Aladin API] 대기 중 인터럽트 발생");
        }
    }

    /**
     * API 요청 URL 생성
     */
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
