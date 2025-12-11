package com.nhnacademy.book_data_batch.batch.components.provider.aladin.client;

import com.nhnacademy.book_data_batch.batch.components.provider.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.components.provider.aladin.dto.api.AladinResponseDto;
import com.nhnacademy.book_data_batch.batch.components.provider.aladin.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
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

    private final RestClient restClient;

    // 신간 채워 넣는 용도
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
    @Retryable(
            retryFor = {RestClientException.class},
            maxAttempts = 2,
            backoff = @Backoff(delay = 1000)
    )
    public Optional<AladinItemDto> lookupByIsbn(String isbn13, String apiKey) {
        String url = buildLookUpUrl(isbn13, apiKey);

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

        return Optional.of(response.item().getFirst());
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
