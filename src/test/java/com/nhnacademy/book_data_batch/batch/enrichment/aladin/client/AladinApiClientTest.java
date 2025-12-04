package com.nhnacademy.book_data_batch.batch.enrichment.aladin.client;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AladinApiClientTest {

    // RETURNS_DEEP_STUBS로 체이닝 메서드 자동 모킹
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    private AladinApiClient aladinApiClient;

    @BeforeEach
    void setUp() {
        aladinApiClient = new AladinApiClient(restClient);
    }

    @Test
    @DisplayName("정상 응답 시 AladinItemDto 반환")
    void lookupByIsbn_success_returnsItem() {
        // given
        AladinItemDto mockItem = new AladinItemDto(
                "테스트 책", "저자", "2024-01-01", "설명",
                15000, "cover.jpg", "국내도서>소설", "출판사", null
        );
        AladinResponseDto mockResponse = new AladinResponseDto(List.of(mockItem));

        given(restClient.get()
                .uri(anyString())
                .retrieve()
                .body(AladinResponseDto.class))
                .willReturn(mockResponse);

        // when
        Optional<AladinItemDto> result = aladinApiClient.lookupByIsbn("9788956746425", "test-key");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("테스트 책");
    }

    @Test
    @DisplayName("응답이 null이면 빈 Optional 반환")
    void lookupByIsbn_nullResponse_returnsEmpty() {
        // given
        given(restClient.get()
                .uri(anyString())
                .retrieve()
                .body(AladinResponseDto.class))
                .willReturn(null);

        // when
        Optional<AladinItemDto> result = aladinApiClient.lookupByIsbn("9788956746425", "test-key");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("응답에 item이 없으면 빈 Optional 반환")
    void lookupByIsbn_emptyItems_returnsEmpty() {
        // given
        AladinResponseDto mockResponse = new AladinResponseDto(List.of());

        given(restClient.get()
                .uri(anyString())
                .retrieve()
                .body(AladinResponseDto.class))
                .willReturn(mockResponse);

        // when
        Optional<AladinItemDto> result = aladinApiClient.lookupByIsbn("9788956746425", "test-key");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("404 등 HTTP 오류 시 빈 Optional 반환")
    void lookupByIsbn_httpError_returnsEmpty() {
        // given
        given(restClient.get()
                .uri(anyString())
                .retrieve()
                .body(AladinResponseDto.class))
                .willThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // when
        Optional<AladinItemDto> result = aladinApiClient.lookupByIsbn("9788956746425", "test-key");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("네트워크 오류 시 재시도 후 빈 Optional 반환")
    void lookupByIsbn_networkError_returnsEmpty() {
        // given
        given(restClient.get()
                .uri(anyString())
                .retrieve()
                .body(AladinResponseDto.class))
                .willThrow(new RestClientException("Connection timeout"));

        // when
        Optional<AladinItemDto> result = aladinApiClient.lookupByIsbn("9788956746425", "test-key");

        // then
        assertThat(result).isEmpty();
    }
}
