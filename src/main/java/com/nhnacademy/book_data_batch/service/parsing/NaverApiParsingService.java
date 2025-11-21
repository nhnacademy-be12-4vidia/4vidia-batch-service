package com.nhnacademy.book_data_batch.service.parsing;

import com.nhnacademy.book_data_batch.dto.naver.NaverApiResponseDto;
import com.nhnacademy.book_data_batch.dto.naver.NaverParsingDto;
import com.nhnacademy.book_data_batch.entity.Author;
import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.entity.Publisher;
import com.nhnacademy.book_data_batch.parser.common.Formatter;
import com.nhnacademy.book_data_batch.service.AuthorService;
import com.nhnacademy.book_data_batch.service.BookAuthorService;
import com.nhnacademy.book_data_batch.service.BookService;
import com.nhnacademy.book_data_batch.service.PublisherService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class NaverApiParsingService {

    private final BookService bookService;
    private final PublisherService publisherService;
    private final AuthorService authorService;
    private final BookAuthorService bookAuthorService;
    private final RestTemplate restTemplate;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

//    @PostConstruct
    void init() {
        searchAndSaveBooks("만화");
    }

    public void searchAndSaveBooks(String keyword) {

        final int display = 100;
        final int totalPages = 5;
        List<String> errorMessage = new ArrayList<>();

        try {
            log.info("------{} 관련 도서 검색 시작------", keyword);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            for (int i = 0; i < totalPages; i++) {
                int start = 1 + (i * display);
                String apiUrl = "https://openapi.naver.com/v1/search/book.json?query=%s&display=%s&start=%s".formatted(
                    keyword, display, start);

                 ResponseEntity<NaverApiResponseDto> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    NaverApiResponseDto.class
                );
                NaverApiResponseDto naverApiResponseDto = response.getBody();

                if (naverApiResponseDto == null || naverApiResponseDto.getItems() == null) return;

                for (NaverParsingDto dto : naverApiResponseDto.getItems()) {
                    log.info(dto.getTitle());
                    List<Author> authors = getAuthors(dto);
                    Publisher publisher = getPublisher(dto);

                    Book book = Book.builder()
                        .title(dto.getTitle())
                        .priceStandard(parseIntFromString(dto.getPrice()))
                        .priceSales(parseIntFromString(dto.getDiscount()))
                        .publisher(publisher)
                        .description(dto.getDescription())
                        .isbn(dto.getIsbn())
                        .publishedDate(parseDateSafe(dto.getPubDate()))
                        .build();

                    try {
                        bookService.createByApi(book);
                        for (Author a : authors) {
                            bookAuthorService.save(book, a);
                        }
                    } catch (IllegalArgumentException e) {
                        log.error(e.getMessage());
                        errorMessage.add(book.getTitle());
                    }

                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LocalDate parseDateSafe(String pubdate) {
        try {
            return StringUtils.hasText(pubdate)
                ? LocalDate.parse(pubdate, Formatter.NAVER_DATE_FORMATTER)
                : LocalDate.of(1900, 1, 1);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private List<Author> getAuthors(NaverParsingDto dto) {
        String[] authorList = getAuthorList(dto);
        List<Author> authors = new ArrayList<>();
        for (String s : authorList) {
            String name = s.trim();
            if (name.isEmpty()) continue;
            if (authorService.isExistsByName(name)) {
                authors.add(authorService.getByName(name));
            } else {
                Author author = Author.builder()
                    .name(name)
                    .build();
                authorService.createByApi(author);
                authors.add(author);
            }
        }
        return authors;
    }

    private Publisher getPublisher(NaverParsingDto dto) {
        String publisherName = dto.getPublisher();
        if (!StringUtils.hasText(publisherName)) {
            publisherName = "출판사 미상";
        }
        if (publisherService.isExists(publisherName)) {
            return publisherService.getByName(publisherName);
        } else {
            Publisher publisher = Publisher.builder()
                .name(publisherName)
                .build();
            return publisherService.createByApi(publisher);
        }
    }

    private String[] getAuthorList(NaverParsingDto dto) {
        String rawString = dto.getAuthor();
        if (!StringUtils.hasText(rawString)) {
            return new String[]{};
        }
        return rawString.split("\\^");
    }

    private Integer parseIntFromString(String value) {
        if (StringUtils.hasText(value)) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else {
            return 0;
        }
    }
}
