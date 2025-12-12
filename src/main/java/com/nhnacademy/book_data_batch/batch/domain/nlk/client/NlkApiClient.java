package com.nhnacademy.book_data_batch.batch.domain.nlk.client;

import com.nhnacademy.book_data_batch.batch.domain.nlk.dto.api.NlkResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NlkApiClient {

    private static final String BASE_URL = "https://www.nl.go.kr/seoji/SearchApi.do";
    private static final String RESULT_STYLE = "json";
    private static final String PAGE_SIZE = "1000";
    private static final String START_PUBLISH_DATE = "20000101";
    private static final String END_PUBLISH_DATE = "20241231";
    private static final String CIP_YN = "Y";
    private static final String FORM = "종이책";
    private static final String SORT = "PUBLISH_PREDATE";
    private static final String ORDER_BY = "desc";

    private final RestClient restClient;

    public Optional<NlkResponseDto> searchBooks(int pageNo, String apiKey) {
        String url = buildSearchUrl(pageNo, apiKey);

        try {
            NlkResponseDto response = restClient
                    .get()
                    .uri(url)
                    .retrieve()
                    .body(NlkResponseDto.class);

            return Optional.ofNullable(response);

        } catch (Exception e) {
            log.error("NL API 호출 실패: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String buildSearchUrl(int pageNo, String apiKey) {
        return UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("cert_key", apiKey)
                .queryParam("result_style", RESULT_STYLE)
                .queryParam("page_no", pageNo)
                .queryParam("page_size", PAGE_SIZE)
                .queryParam("start_publish_date", START_PUBLISH_DATE)
                .queryParam("end_publish_date", END_PUBLISH_DATE)
                .queryParam("cip_yn", CIP_YN)
                .queryParam("form", FORM)
                .queryParam("sort", SORT)
                .queryParam("order_by", ORDER_BY)
                .toUriString();
    }
}
