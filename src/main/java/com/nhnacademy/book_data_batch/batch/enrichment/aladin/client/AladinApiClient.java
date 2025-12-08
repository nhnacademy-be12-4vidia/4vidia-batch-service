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

import java.util.List;
import java.util.Optional;

/**
 * Aladin ItemLookUp API 클라이언트
 * - ISBN13으로 도서 상세 정보 조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AladinApiClient {

    private static final String LIST_URL = "http://www.aladin.co.kr/ttb/api/ItemList.aspx";
    private static final String QUERY_TYPE = "Title";
    private static final String SEARCH_TARGET = "Book";
    private static final String MAX_RESULTS = "100";
    private static final String LOOK_UP_URL = "http://www.aladin.co.kr/ttb/api/ItemLookUp.aspx";
    private static final String ITEM_ID_TYPE = "ISBN13";
    private static final String COVER_SIZE = "Big";
    private static final String OUTPUT_FORMAT = "JS";
    private static final String VERSION = "20131101";
    private static final String OUT_OF_STOCK_FILTER = "1";

    // API 호출 설정
    private static final long API_CALL_DELAY_MS = 200;       // 호출 간 딜레이 (100ms)
    
    // 429 Rate Limit 재시도 설정
    private static final int MAX_RETRY_429 = 3;              // 최대 재시도 횟수
    private static final long RATE_LIMIT_WAIT_MS = 60_000;   // 대기 시간 (1분)
    
    // 네트워크 오류 재시도 설정
    private static final int MAX_RETRY_NETWORK = 1;          // 최대 재시도 횟수
    private static final long NETWORK_RETRY_WAIT_MS = 1_000; // 대기 시간 (1초)

    private final RestClient restClient;

    // 알라딘에서 리스트 받아서 신간으로 할까 했는데, 국립도서관에서 더 많이 준다는 걸 알게돼서
    // 안 쓰게될 것 같
    public Optional<List<AladinItemDto>> listItems(int start, String apiKey) {
        String url = buildListUrl(start, apiKey);

        try {
            AladinResponseDto response = restClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .body(AladinResponseDto.class);

            if (response == null || response.hasError() || response.item() == null) {
                log.debug("[Aladin API] 목록 조회 실패 또는 결과 없음: start={}", start);
                return Optional.empty();
            }

            return Optional.of(response.item());

        } catch (RestClientException e) {
            log.warn("[Aladin API] 목록 조회 중 오류 발생: start={}, error={}", start, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * ISBN13으로 도서 상세 정보 조회 (재시도 로직 포함)
     *
     * @param isbn13 조회할 도서의 ISBN13
     * @param apiKey 사용할 Aladin API 키
     * @return 조회된 도서 정보 (없으면 빈 Optional)
     */
    public Optional<AladinItemDto> lookupByIsbn(String isbn13, String apiKey) {
        String url = buildLookUpUrl(isbn13, apiKey);

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

                if (response == null) {
                    log.debug("[Aladin API] 응답 없음: ISBN={}", isbn13);
                    return Optional.empty();
                }

                // 에러 응답 체크 (200 OK로 에러가 오는 경우)
                if (response.hasError()) {
                    if (response.isQuotaExceeded()) {
                        // 쿼터 초과: 해당 API 키로 더 이상 호출 불가
                        log.warn("[Aladin API] 쿼터 초과: ISBN={}, error={}", isbn13, response.errorMessage());
                        throw new RateLimitExceededException(apiKey, response.errorCode(),
                                "[Aladin API] 일일 쿼터 초과: " + response.errorMessage());
                    }
                    log.debug("[Aladin API] API 에러: ISBN={}, code={}, msg={}", 
                            isbn13, response.errorCode(), response.errorMessage());
                    return Optional.empty();
                }

                if (response.item() == null || response.item().isEmpty()) {
                    log.debug("[Aladin API] 검색 결과 없음: ISBN={}", isbn13);
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

    private String buildListUrl(int start, String apiKey) {
        return UriComponentsBuilder.fromUriString(LIST_URL)
                .queryParam("ttbkey", apiKey)
                .queryParam("QueryType", QUERY_TYPE)
                .queryParam("SearchTarget", SEARCH_TARGET)
                .queryParam("Start", start)
                .queryParam("MaxResults", MAX_RESULTS)
                .queryParam("Cover", COVER_SIZE)
                .queryParam("Output", OUTPUT_FORMAT)
                .queryParam("Version", VERSION)
                .queryParam("OutOfStock", OUT_OF_STOCK_FILTER)
                .build()
                .toUriString();
    }

    private String buildLookUpUrl(String isbn13, String apiKey) {
        return UriComponentsBuilder.fromUriString(LOOK_UP_URL)
                .queryParam("ttbkey", apiKey)
                .queryParam("ItemIdType", ITEM_ID_TYPE)
                .queryParam("ItemId", isbn13)
                .queryParam("Cover", COVER_SIZE)
                .queryParam("Output", OUTPUT_FORMAT)
                .queryParam("Version", VERSION)
                .build()
                .toUriString();
    }
}
