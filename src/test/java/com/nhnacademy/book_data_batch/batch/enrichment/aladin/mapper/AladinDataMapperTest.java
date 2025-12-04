package com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper;

import com.nhnacademy.book_data_batch.batch.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinBookInfoDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.extractor.AuthorExtractor;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.extractor.CategoryTagExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AladinDataMapperTest {

    private AladinDataMapper mapper;

    @BeforeEach
    void setUp() {
        // 실제 Extractor 사용 (단순한 로직이라 Mock 불필요)
        mapper = new AladinDataMapper(new AuthorExtractor(), new CategoryTagExtractor());
    }

    @Test
    @DisplayName("전체 데이터가 있는 AladinItemDto를 EnrichmentSuccessDto로 변환")
    void map_fullData_success() {
        // given
        BookBatchTarget target = new BookBatchTarget(1L, "9788956746425", 100L);
        AladinItemDto item = createFullAladinItem();

        // when
        EnrichmentSuccessDto result = mapper.map(target, item);

        // then
        assertThat(result.bookId()).isEqualTo(1L);
        assertThat(result.batchId()).isEqualTo(100L);
        assertThat(result.description()).isEqualTo("이것은 테스트 도서 설명입니다.");
        assertThat(result.priceStandard()).isEqualTo(18000);
        assertThat(result.publishedDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        assertThat(result.subtitle()).isEqualTo("부제목입니다");
        assertThat(result.pageCount()).isEqualTo(320);
        assertThat(result.bookIndex()).isEqualTo("1장. 서론\n2장. 본론");
        assertThat(result.coverUrl()).isEqualTo("https://image.aladin.co.kr/cover.jpg");
        assertThat(result.language()).isEqualTo("ko");

        // 저자 검증
        assertThat(result.authors()).hasSize(2);
        assertThat(result.authors().getFirst().name()).isEqualTo("홍길동");
        assertThat(result.authors().getFirst().role()).isEqualTo("지은이");

        // 태그 검증
        assertThat(result.tags()).containsExactly("국내도서", "소설", "한국소설");
    }

    @Test
    @DisplayName("bookinfo가 없어도 기본 데이터는 변환됨")
    void map_noBookInfo_partialSuccess() {
        // given
        BookBatchTarget target = new BookBatchTarget(2L, "9788956746426", 101L);
        AladinItemDto item = new AladinItemDto(
                "제목",
                "작가명",
                "2024-06-01",
                "설명",
                15000,
                "cover.jpg",
                "국내도서>에세이",
                "출판사",
                null  // bookinfo 없음
        );

        // when
        EnrichmentSuccessDto result = mapper.map(target, item);

        // then
        assertThat(result.bookId()).isEqualTo(2L);
        assertThat(result.description()).isEqualTo("설명");
        assertThat(result.priceStandard()).isEqualTo(15000);
        assertThat(result.publishedDate()).isEqualTo(LocalDate.of(2024, 6, 1));
        assertThat(result.subtitle()).isNull();
        assertThat(result.pageCount()).isNull();
        assertThat(result.tags()).containsExactly("국내도서", "에세이");
    }

    @Test
    @DisplayName("잘못된 날짜 형식은 null로 처리")
    void map_invalidDate_returnsNull() {
        // given
        BookBatchTarget target = new BookBatchTarget(3L, "9788956746427", 102L);
        AladinItemDto item = new AladinItemDto(
                "제목", "작가", "invalid-date", "설명",
                10000, null, null, "출판사", null
        );

        // when
        EnrichmentSuccessDto result = mapper.map(target, item);

        // then
        assertThat(result.publishedDate()).isNull();
    }

    @Test
    @DisplayName("외국 도서는 language가 null")
    void map_foreignBook_languageNull() {
        // given
        BookBatchTarget target = new BookBatchTarget(4L, "9780123456789", 103L);
        AladinItemDto item = new AladinItemDto(
                "English Book", "Author", "2024-01-01", "Description",
                25000, null, "외국도서>영미소설", "Publisher", null
        );

        // when
        EnrichmentSuccessDto result = mapper.map(target, item);

        // then
        assertThat(result.language()).isNull();  // 국내도서가 아니므로 null
    }

    private AladinItemDto createFullAladinItem() {
        return new AladinItemDto(
                "테스트 책 제목",
                "홍길동 (지은이), 김철수 (옮긴이)",
                "2024-01-15",
                "이것은 테스트 도서 설명입니다.",
                18000,
                "https://image.aladin.co.kr/cover.jpg",
                "국내도서>소설>한국소설",
                "테스트출판사",
                new AladinBookInfoDto(
                        "부제목입니다",
                        320,
                        "1장. 서론\n2장. 본론",
                        List.of(
                                new AladinBookInfoDto.AladinBookAuthorDto("홍길동", "지은이"),
                                new AladinBookInfoDto.AladinBookAuthorDto("김철수", "옮긴이")
                        )
                )
        );
    }
}
