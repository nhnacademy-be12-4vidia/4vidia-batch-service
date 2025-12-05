package com.nhnacademy.book_data_batch.batch.enrichment.aladin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.EnrichmentSuccessDto.AuthorWithRole;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinBookInfoDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.api.AladinResponseDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.extractor.AuthorExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Aladin API 응답이 DTO에 제대로 매핑되는지 확인하는 테스트
 */
class AladinApiResponseMappingTest {

    private ObjectMapper objectMapper;
    private AuthorExtractor authorExtractor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        authorExtractor = new AuthorExtractor();
    }

    /**
     * 실제 알라딘 API 응답 형태의 JSON
     */
    private static final String REAL_ALADIN_RESPONSE_JSON = """
            {
                "version": "20070901",
                "title": "알라딘 상품정보 - 자바 ORM 표준 JPA 프로그래밍",
                "link": "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=62681446",
                "pubDate": "Thu, 20 Nov 2025 04:55:52 GMT",
                "imageUrl": "http://www.aladin.co.kr/ucl_editor/img_secur/header/2010/logo.jpg",
                "totalResults": 1,
                "startIndex": 1,
                "itemsPerPage": 1,
                "query": "isbn13=9788960777330",
                "searchCategoryId": 0,
                "searchCategoryName": "",
                "item": [
                    {
                        "title": "자바 ORM 표준 JPA 프로그래밍 - 스프링 데이터 예제 프로젝트로 배우는 전자정부 표준 데이터베이스 프레임워크",
                        "link": "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=62681446",
                        "author": "김영한 지음",
                        "pubDate": "2015-07-27",
                        "description": "에이콘 오픈 소스 프로그래밍 시리즈. 이 책은 JPA 기초 이론과 핵심 원리, 그리고 실무에 필요한 성능 최적화 방법까지 JPA에 대한 모든 것을 다룬다.",
                        "creator": "aladin",
                        "isbn": "8960777331",
                        "isbn13": "9788960777330",
                        "itemId": 62681446,
                        "priceSales": 38700,
                        "priceStandard": 43000,
                        "stockStatus": "",
                        "mileage": 2150,
                        "cover": "https://image.aladin.co.kr/product/6268/14/coversum/8960777331_1.jpg",
                        "categoryId": 2502,
                        "categoryName": "국내도서>컴퓨터/모바일>프로그래밍 언어>자바",
                        "publisher": "에이콘출판",
                        "customerReviewRank": 9,
                        "bookinfo": {
                            "subTitle": "스프링 데이터 예제 프로젝트로 배우는 전자정부 표준 데이터베이스 프레임워크",
                            "originalTitle": "",
                            "itemPage": 736,
                            "toc": "1장. JPA 소개...",
                            "letslookimg": [
                                "https://image.aladin.co.kr/product/6268/14/letslook/8960777331_fs.jpg"
                            ],
                            "authors": [
                                {
                                    "authorType": "author",
                                    "authorid": 4263873,
                                    "desc": "지은이",
                                    "name": "김영한"
                                }
                            ],
                            "ebookList": [
                                {
                                    "itemId": 241970388,
                                    "isbn": "E982537409",
                                    "priceSales": 34400,
                                    "link": "https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=241970388"
                                }
                            ]
                        }
                    }
                ]
            }
            """;

    /**
     * 저자가 여러 명인 응답 JSON
     */
    private static final String MULTI_AUTHORS_RESPONSE_JSON = """
            {
                "version": "20070901",
                "item": [
                    {
                        "title": "테스트 책",
                        "author": "저자1, 저자2 지음",
                        "pubDate": "2024-01-01",
                        "description": "테스트 설명",
                        "priceStandard": 30000,
                        "cover": "https://example.com/cover.jpg",
                        "categoryName": "국내도서>컴퓨터",
                        "publisher": "테스트출판사",
                        "bookinfo": {
                            "subTitle": "서브타이틀",
                            "itemPage": 300,
                            "toc": "목차...",
                            "authors": [
                                {
                                    "authorType": "author",
                                    "authorid": 1,
                                    "desc": "지은이",
                                    "name": "저자1"
                                },
                                {
                                    "authorType": "author",
                                    "authorid": 2,
                                    "desc": "지은이",
                                    "name": "저자2"
                                },
                                {
                                    "authorType": "translator",
                                    "authorid": 3,
                                    "desc": "옮긴이",
                                    "name": "번역자"
                                }
                            ]
                        }
                    }
                ]
            }
            """;

    /**
     * bookinfo.authors가 없고 item.author만 있는 응답 JSON
     */
    private static final String NO_BOOKINFO_AUTHORS_JSON = """
            {
                "version": "20070901",
                "item": [
                    {
                        "title": "테스트 책",
                        "author": "홍길동 지음",
                        "pubDate": "2024-01-01",
                        "description": "테스트 설명",
                        "priceStandard": 25000,
                        "cover": "https://example.com/cover.jpg",
                        "categoryName": "국내도서>소설",
                        "publisher": "테스트출판사",
                        "bookinfo": {
                            "subTitle": "서브타이틀",
                            "itemPage": 200,
                            "toc": "목차..."
                        }
                    }
                ]
            }
            """;

    /**
     * bookinfo 자체가 null인 응답 JSON
     */
    private static final String NULL_BOOKINFO_JSON = """
            {
                "version": "20070901",
                "item": [
                    {
                        "title": "테스트 책",
                        "author": "작가명 지음",
                        "pubDate": "2024-01-01",
                        "description": "테스트 설명",
                        "priceStandard": 20000,
                        "cover": "https://example.com/cover.jpg",
                        "categoryName": "국내도서>에세이",
                        "publisher": "테스트출판사"
                    }
                ]
            }
            """;

    @Test
    @DisplayName("실제 알라딘 API 응답 JSON이 AladinResponseDto로 정상 매핑되어야 한다")
    void testRealAladinResponseMapping() throws Exception {
        // when
        AladinResponseDto response = objectMapper.readValue(REAL_ALADIN_RESPONSE_JSON, AladinResponseDto.class);

        // then
        assertThat(response).isNotNull();
        assertThat(response.hasError()).isFalse();
        assertThat(response.item()).isNotNull().hasSize(1);

        AladinItemDto item = response.item().get(0);
        assertThat(item.title()).contains("자바 ORM 표준 JPA 프로그래밍");
        assertThat(item.author()).isEqualTo("김영한 지음");
        assertThat(item.publisher()).isEqualTo("에이콘출판");
        assertThat(item.priceStandard()).isEqualTo(43000);
    }

    @Test
    @DisplayName("bookinfo.authors가 정상 매핑되어야 한다")
    void testBookInfoAuthorsMapping() throws Exception {
        // when
        AladinResponseDto response = objectMapper.readValue(REAL_ALADIN_RESPONSE_JSON, AladinResponseDto.class);
        AladinItemDto item = response.item().get(0);

        // then
        assertThat(item.bookinfo()).isNotNull();
        assertThat(item.bookinfo().authors()).isNotNull().hasSize(1);

        AladinBookInfoDto.AladinBookAuthorDto author = item.bookinfo().authors().get(0);
        assertThat(author.name()).isEqualTo("김영한");
        assertThat(author.desc()).isEqualTo("지은이");
    }

    @Test
    @DisplayName("AuthorExtractor가 bookinfo.authors에서 저자를 올바르게 추출해야 한다")
    void testAuthorExtractorFromBookInfo() throws Exception {
        // given
        AladinResponseDto response = objectMapper.readValue(REAL_ALADIN_RESPONSE_JSON, AladinResponseDto.class);
        AladinItemDto item = response.item().get(0);

        // when
        List<AuthorWithRole> authors = authorExtractor.extract(item);

        // then
        assertThat(authors).isNotNull().hasSize(1);
        assertThat(authors.get(0).name()).isEqualTo("김영한");
        assertThat(authors.get(0).role()).isEqualTo("지은이");
    }

    @Test
    @DisplayName("여러 저자가 있는 응답이 정상 매핑되어야 한다")
    void testMultiAuthorsMapping() throws Exception {
        // when
        AladinResponseDto response = objectMapper.readValue(MULTI_AUTHORS_RESPONSE_JSON, AladinResponseDto.class);
        AladinItemDto item = response.item().get(0);

        // then
        assertThat(item.bookinfo()).isNotNull();
        assertThat(item.bookinfo().authors()).hasSize(3);

        List<AuthorWithRole> authors = authorExtractor.extract(item);
        assertThat(authors).hasSize(3);
        
        // 첫 번째 저자
        assertThat(authors.get(0).name()).isEqualTo("저자1");
        assertThat(authors.get(0).role()).isEqualTo("지은이");
        
        // 두 번째 저자
        assertThat(authors.get(1).name()).isEqualTo("저자2");
        assertThat(authors.get(1).role()).isEqualTo("지은이");
        
        // 번역자
        assertThat(authors.get(2).name()).isEqualTo("번역자");
        assertThat(authors.get(2).role()).isEqualTo("옮긴이");
    }

    @Test
    @DisplayName("bookinfo.authors가 없으면 빈 리스트를 반환해야 한다 (현재 parseAuthorString 미구현)")
    void testNoBookInfoAuthors() throws Exception {
        // given
        AladinResponseDto response = objectMapper.readValue(NO_BOOKINFO_AUTHORS_JSON, AladinResponseDto.class);
        AladinItemDto item = response.item().get(0);

        // then - bookinfo는 있지만 authors가 null
        assertThat(item.bookinfo()).isNotNull();
        assertThat(item.bookinfo().authors()).isNull();
        
        // AuthorExtractor - parseAuthorString이 미구현이라 빈 리스트 반환
        List<AuthorWithRole> authors = authorExtractor.extract(item);
        assertThat(authors).isEmpty();
        
        // 하지만 item.author()는 존재함 - 이 값을 파싱해야 함
        assertThat(item.author()).isEqualTo("홍길동 지음");
    }

    @Test
    @DisplayName("bookinfo가 null이면 빈 리스트를 반환해야 한다")
    void testNullBookInfo() throws Exception {
        // given
        AladinResponseDto response = objectMapper.readValue(NULL_BOOKINFO_JSON, AladinResponseDto.class);
        AladinItemDto item = response.item().get(0);

        // then
        assertThat(item.bookinfo()).isNull();
        
        // AuthorExtractor - parseAuthorString이 미구현이라 빈 리스트 반환
        List<AuthorWithRole> authors = authorExtractor.extract(item);
        assertThat(authors).isEmpty();
        
        // 하지만 item.author()는 존재함
        assertThat(item.author()).isEqualTo("작가명 지음");
    }

    @Test
    @DisplayName("bookinfo의 다른 필드들도 정상 매핑되어야 한다")
    void testBookInfoOtherFieldsMapping() throws Exception {
        // when
        AladinResponseDto response = objectMapper.readValue(REAL_ALADIN_RESPONSE_JSON, AladinResponseDto.class);
        AladinItemDto item = response.item().get(0);
        AladinBookInfoDto bookinfo = item.bookinfo();

        // then
        assertThat(bookinfo.subTitle()).isEqualTo("스프링 데이터 예제 프로젝트로 배우는 전자정부 표준 데이터베이스 프레임워크");
        assertThat(bookinfo.itemPage()).isEqualTo(736);
        assertThat(bookinfo.toc()).isNotNull();
    }

    @Test
    @DisplayName("에러 응답이 정상 매핑되어야 한다")
    void testErrorResponseMapping() throws Exception {
        // given
        String errorJson = """
                {
                    "version": "20070901",
                    "errorCode": 10,
                    "errorMessage": "일일 요청수를 초과하였습니다."
                }
                """;

        // when
        AladinResponseDto response = objectMapper.readValue(errorJson, AladinResponseDto.class);

        // then
        assertThat(response.hasError()).isTrue();
        assertThat(response.isQuotaExceeded()).isTrue();
        assertThat(response.errorCode()).isEqualTo(10);
        assertThat(response.errorMessage()).isEqualTo("일일 요청수를 초과하였습니다.");
        assertThat(response.item()).isNull();
    }

    @Test
    @DisplayName("알 수 없는 필드가 있어도 @JsonIgnoreProperties로 무시되어야 한다")
    void testUnknownFieldsIgnored() throws Exception {
        // given - 알 수 없는 필드들이 많이 포함된 응답
        String jsonWithUnknownFields = """
                {
                    "version": "20070901",
                    "unknownField1": "value1",
                    "unknownField2": 12345,
                    "item": [
                        {
                            "title": "테스트",
                            "author": "테스트작가",
                            "unknownItemField": "unknown",
                            "pubDate": "2024-01-01",
                            "description": "설명",
                            "priceStandard": 10000,
                            "cover": "cover.jpg",
                            "categoryName": "카테고리",
                            "publisher": "출판사",
                            "bookinfo": {
                                "subTitle": "서브",
                                "itemPage": 100,
                                "toc": "목차",
                                "unknownBookInfoField": "unknown",
                                "authors": [
                                    {
                                        "name": "저자",
                                        "desc": "지은이",
                                        "unknownAuthorField": "unknown"
                                    }
                                ]
                            }
                        }
                    ]
                }
                """;

        // when
        AladinResponseDto response = objectMapper.readValue(jsonWithUnknownFields, AladinResponseDto.class);

        // then - 정상 파싱됨
        assertThat(response).isNotNull();
        assertThat(response.item()).hasSize(1);
        assertThat(response.item().get(0).bookinfo().authors()).hasSize(1);
        assertThat(response.item().get(0).bookinfo().authors().get(0).name()).isEqualTo("저자");
    }

    @Test
    @DisplayName("authors 배열이 빈 배열이면 빈 리스트를 반환해야 한다")
    void testEmptyAuthorsArray() throws Exception {
        // given
        String emptyAuthorsJson = """
                {
                    "version": "20070901",
                    "item": [
                        {
                            "title": "테스트",
                            "author": "작가 지음",
                            "pubDate": "2024-01-01",
                            "description": "설명",
                            "priceStandard": 10000,
                            "cover": "cover.jpg",
                            "categoryName": "카테고리",
                            "publisher": "출판사",
                            "bookinfo": {
                                "subTitle": "서브",
                                "itemPage": 100,
                                "toc": "목차",
                                "authors": []
                            }
                        }
                    ]
                }
                """;

        // when
        AladinResponseDto response = objectMapper.readValue(emptyAuthorsJson, AladinResponseDto.class);
        AladinItemDto item = response.item().get(0);

        // then
        assertThat(item.bookinfo().authors()).isEmpty();
        
        List<AuthorWithRole> authors = authorExtractor.extract(item);
        assertThat(authors).isEmpty();
    }

    @Test
    @DisplayName("저자 이름이 공백이면 필터링되어야 한다")
    void testBlankAuthorNameFiltered() throws Exception {
        // given
        String blankAuthorNameJson = """
                {
                    "version": "20070901",
                    "item": [
                        {
                            "title": "테스트",
                            "author": "작가 지음",
                            "pubDate": "2024-01-01",
                            "description": "설명",
                            "priceStandard": 10000,
                            "cover": "cover.jpg",
                            "categoryName": "카테고리",
                            "publisher": "출판사",
                            "bookinfo": {
                                "subTitle": "서브",
                                "itemPage": 100,
                                "toc": "목차",
                                "authors": [
                                    {
                                        "name": "",
                                        "desc": "지은이"
                                    },
                                    {
                                        "name": "  ",
                                        "desc": "옮긴이"
                                    },
                                    {
                                        "name": "정상작가",
                                        "desc": "지은이"
                                    }
                                ]
                            }
                        }
                    ]
                }
                """;

        // when
        AladinResponseDto response = objectMapper.readValue(blankAuthorNameJson, AladinResponseDto.class);
        AladinItemDto item = response.item().get(0);
        List<AuthorWithRole> authors = authorExtractor.extract(item);

        // then - 빈 이름과 공백만 있는 저자는 필터링됨
        assertThat(authors).hasSize(1);
        assertThat(authors.get(0).name()).isEqualTo("정상작가");
    }

    @Test
    @DisplayName("저자 역할(desc)이 null이면 기본값 '지은이'가 사용되어야 한다")
    void testNullDescDefaultsToAuthor() throws Exception {
        // given
        String nullDescJson = """
                {
                    "version": "20070901",
                    "item": [
                        {
                            "title": "테스트",
                            "author": "작가 지음",
                            "pubDate": "2024-01-01",
                            "description": "설명",
                            "priceStandard": 10000,
                            "cover": "cover.jpg",
                            "categoryName": "카테고리",
                            "publisher": "출판사",
                            "bookinfo": {
                                "subTitle": "서브",
                                "itemPage": 100,
                                "toc": "목차",
                                "authors": [
                                    {
                                        "name": "작가명"
                                    }
                                ]
                            }
                        }
                    ]
                }
                """;

        // when
        AladinResponseDto response = objectMapper.readValue(nullDescJson, AladinResponseDto.class);
        AladinItemDto item = response.item().get(0);
        List<AuthorWithRole> authors = authorExtractor.extract(item);

        // then
        assertThat(authors).hasSize(1);
        assertThat(authors.get(0).name()).isEqualTo("작가명");
        assertThat(authors.get(0).role()).isEqualTo("지은이"); // 기본값
    }
}
