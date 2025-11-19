package init.data.DataParser.service.parsing;

import init.data.DataParser.DTO.Aladin.AladinAuthorWithRoleDto;
import init.data.DataParser.DTO.Aladin.AladinItemDto;
import init.data.DataParser.DTO.Aladin.AladinResponseDto;
import init.data.DataParser.constant.AladinCategory;
import init.data.DataParser.entity.Author;
import init.data.DataParser.entity.Book;
import init.data.DataParser.entity.BookAuthor;
import init.data.DataParser.entity.Publisher;
import init.data.DataParser.parser.common.Formatter;
import init.data.DataParser.service.AuthorService;
import init.data.DataParser.service.BookAuthorService;
import init.data.DataParser.service.BookService;
import init.data.DataParser.service.PublisherService;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AladinApiParsingService {

    private final BookService bookService;
    private final AuthorService authorService;
    private final PublisherService publisherService;
    private final BookAuthorService bookAuthorService;
    private final RestTemplate restTemplate;

    @Value("${aladin.api.key}")
    private String aladinKey;

    @PostConstruct
    void init() {
        searchAndSaveBooks("만화");
    }


    public void searchAndSaveBooks(String keyword) {
        final int maxResults = 50;
        final int totalPages = 10;
        int categoryId = AladinCategory.findCidByKorean(keyword);

        try {
            log.info("------{} 관련 도서 검색 시작------", keyword);

            for (int i = 0; i < totalPages; i++) {
                int pageNum = i + 1;

                String apiUrl = "http://www.aladin.co.kr/ttb/api/ItemList.aspx?ttbkey=%s&QueryType=ItemNewSpecial&MaxResults=%d&start=%d&SearchTarget=Book&output=js&Version=20131101&CategoryId=%d"
                    .formatted(aladinKey, maxResults, pageNum, categoryId);

                ResponseEntity<AladinResponseDto> response = restTemplate.getForEntity(apiUrl,
                    AladinResponseDto.class);

                AladinResponseDto aladinResponseDto = response.getBody();

                if (aladinResponseDto == null || aladinResponseDto.getItem() == null) break;

                for (AladinItemDto dto : aladinResponseDto.getItem()) {
                    List<AladinAuthorWithRoleDto> withRoleDtos = getAuthorsWithRole(dto);

                    Publisher publisher = getPublisher(dto);

                    Book book = Book.builder()
                        .title(dto.getTitle())
                        .priceStandard(dto.getPriceStandard())
                        .priceSales(dto.getPriceSales())
                        .publisher(publisher)
                        .description(dto.getDescription())
                        .isbn(dto.getIsbn13())
                        .publishedDate(parseDateSafe(dto.getPubDate()))
                        .build();

                    try {
                        bookService.createByApi(book);
                        for (AladinAuthorWithRoleDto withRoleDto : withRoleDtos) {
                            BookAuthor bookAuthor = BookAuthor.builder()
                                .author(withRoleDto.getAuthor())
                                .role(withRoleDto.getRole())
                                .book(book)
                                .build();
                            bookAuthorService.createByApi(bookAuthor);
                        }
                    } catch (IllegalArgumentException e) {
                        log.error(e.getMessage());
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
                ? LocalDate.parse(pubdate, Formatter.DEFAULT_DATE_FORMATTER)
                : LocalDate.of(1900, 1, 1);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private List<AladinAuthorWithRoleDto> getAuthorsWithRole(AladinItemDto dto) {
        String rawAuthor = dto.getAuthor();
        if (!StringUtils.hasText(rawAuthor)) return new ArrayList<>();

        String[] authorList = rawAuthor.split(",");
        List<AladinAuthorWithRoleDto> result = new ArrayList<>();

        for (String s : authorList) {
            String token = s.trim();
            if (token.isEmpty()) continue;

            String name;
            String role;

            int parentIndex = token.lastIndexOf('(');

            if (parentIndex > 0) {
                name = token.substring(0, parentIndex).trim();
                role = token.substring(parentIndex + 1).replace(")", "").trim();
            } else {
                name = token;
                role = "지은이";
            }

            Author author;
            if (authorService.isExistsByName(name)) {
                author = authorService.getByName(name);
            } else {
                author = Author.builder().name(name).build();
                authorService.createByApi(author);
            }

            result.add(new AladinAuthorWithRoleDto(author, role));
        }

        return result;
    }

    private Publisher getPublisher(AladinItemDto dto) {
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

    private String[] getAuthorList(AladinItemDto dto) {
        String rawString = dto.getAuthor();
        if (!StringUtils.hasText(rawString)) {
            return new String[]{};
        }
        return rawString.split(",");
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
