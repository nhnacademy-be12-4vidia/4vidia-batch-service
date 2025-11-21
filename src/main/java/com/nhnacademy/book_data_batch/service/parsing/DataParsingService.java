package com.nhnacademy.book_data_batch.service.parsing;

import com.nhnacademy.book_data_batch.dto.ParsingDto;
import com.nhnacademy.book_data_batch.entity.Author;
import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.entity.BookAuthor;
import com.nhnacademy.book_data_batch.entity.Publisher;
import com.nhnacademy.book_data_batch.parser.DataParser;
import com.nhnacademy.book_data_batch.parser.DataParserResolver;
import com.nhnacademy.book_data_batch.parser.common.Formatter;
import com.nhnacademy.book_data_batch.repository.AuthorRepository;
import com.nhnacademy.book_data_batch.repository.BookAuthorRepository;
import com.nhnacademy.book_data_batch.repository.BookRepository;
import com.nhnacademy.book_data_batch.repository.PublisherRepository;
import com.nhnacademy.book_data_batch.service.AuthorService;
import com.nhnacademy.book_data_batch.service.BookAuthorService;
import com.nhnacademy.book_data_batch.service.BookService;
import com.nhnacademy.book_data_batch.service.PublisherService;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DataParsingService {

    private final BookService bookService;
    private final AuthorService authorService;
    private final BookAuthorService bookAuthorService;
    private final PublisherService publisherService;
    private final DataParserResolver dataParserResolver;
    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final BookRepository bookRepository;
    private final BookAuthorRepository bookAuthorRepository;


//    @PostConstruct
    public void init() {
        try {
            loadData("classpath:data/*.*");
        } catch (IOException e) {
            log.error("데이터 로드 실패", e);
        }

    }

    public void loadData(String location) throws IOException {
        PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

        Resource[] resources = resourceResolver.getResources(location);

        for (Resource r : resources) {
            String fileName = r.getFilename();
            if (fileName == null) {
                continue;
            }

            DataParser parser = dataParserResolver.getDataParser(fileName);

            if (parser != null) {
                File file = r.getFile();
                List<ParsingDto> records = parser.parsing(file);
                saveDataBulk(records);
            } else {
                log.error("지원하는 parser가 없습니다.");
            }
        }
    }

    private void saveData(List<ParsingDto> records) throws IOException {
        for (ParsingDto dto : records) {
            try {
                Book book;
                Author author;
                List<String> authorList = dto.getAuthors();

                book = bookService.create(dto);

                for (String name : authorList) {
                    author = authorService.save(name);
                    BookAuthor bookAuthor = bookAuthorService.save(book, author);
                }
                Publisher publisher = publisherService.save(dto);
            } catch (IllegalArgumentException e) {
                log.error("무언가 잘못되었습니다.");
            } catch (Exception e) {
                log.error("무언가 완전히 잘못되었습니다.");
            }
        }
    }

    private void saveDataBulk(List<ParsingDto> records) {
        Set<String> allAuthorNames = new HashSet<>();
        Set<String> allPublisherNames = new HashSet<>();
        Set<String> allIsbns = new HashSet<>();

        for (ParsingDto dto : records) {
            if (dto.getAuthors() != null)
                allAuthorNames.addAll(dto.getAuthors());
            if (dto.getPublisher() != null)
                allPublisherNames.add(dto.getPublisher());
            if (dto.getIsbn() != null)
                allIsbns.add(dto.getIsbn());
        }

        // ==========================================
        // 2. [작가 처리] DB 조회 -> 없는 사람만 골라내기 -> 저장
        // ==========================================
        // 2-1. DB에 있는 작가 한방에 가져오기
        List<Author> existingAuthors = authorRepository.findAllByNameIn(allAuthorNames);

        // 2-2. 빠른 조회를 위해 Map으로 변환 (이름 -> Author 객체)
        Map<String, Author> authorMap = existingAuthors.stream()
            .collect(Collectors.toMap(Author::getName, a -> a));

        // 2-3. DB에 없는 새로운 작가만 리스트에 담기
        List<Author> newAuthors = new ArrayList<>();
        for (String name : allAuthorNames) {
            if (!authorMap.containsKey(name)) {
                Author newAuthor = Author.builder().name(name).build();
                newAuthors.add(newAuthor);
                // 나중에 Map에서 꺼내 쓸 수 있게 미리 넣어둠 (ID는 아직 없음)
                authorMap.put(name, newAuthor);
            }
        }

        // 2-4. 신규 작가만 한방에 저장 (이때 ID 생성됨)
        authorRepository.saveAll(newAuthors);

        // ==========================================
        // 3. [출판사 처리] (작가와 동일한 로직)
        // ==========================================
        List<Publisher> existingPublishers = publisherRepository.findAllByNameIn(allPublisherNames);
        Map<String, Publisher> publisherMap = existingPublishers.stream()
            .collect(Collectors.toMap(Publisher::getName, p -> p));

        List<Publisher> newPublishers = new ArrayList<>();
        for (String name : allPublisherNames) {
            if (name != null && !publisherMap.containsKey(name)) {
                Publisher newPub = Publisher.builder().name(name).build();
                newPublishers.add(newPub);
                publisherMap.put(name, newPub);
            }
        }
        publisherRepository.saveAll(newPublishers);

        // ==========================================
        // 4. [책 처리] 이미 있는 ISBN 제외하고 저장
        // ==========================================
        // 4-1. 이미 등록된 책 조회
        List<Book> existingBooks = bookRepository.findAllByIsbnIn(allIsbns);
        Set<String> existingIsbns = existingBooks.stream()
            .map(Book::getIsbn)
            .collect(Collectors.toSet());

        List<Book> newBooks = new ArrayList<>();
        // 나중에 BookAuthor 연결을 위해 DTO 인덱스와 매칭할 임시 리스트
        List<ParsingDto> targetDtos = new ArrayList<>();

        for (ParsingDto dto : records) {
            // 이미 있는 책이면 스킵 (업데이트 로직이 필요하면 여기서 처리)
            if (existingIsbns.contains(dto.getIsbn())) {
                continue;
            }

            Publisher publisher = publisherMap.get(dto.getPublisher());

            Book book = Book.builder()
                .title(dto.getTitle())
                .isbn(dto.getIsbn())
                .description(dto.getDescription())
                .priceStandard(dto.getPriceStandard())
                .publishedDate(parseDateSafe(dto.getPublishedDate()))
                .publisher(publisher)
                .build();

            newBooks.add(book);
            targetDtos.add(dto); // 저장할 책과 짝이 되는 DTO도 순서대로 저장
        }

        for (int i = 0; i < 10; i++) {
            log.info(newBooks.get(i).getTitle());
        }

        // 4-2. 책 한방에 저장
        List<Book> savedBooks = bookRepository.saveAll(newBooks);

        // ==========================================
        // 5. [책-작가 연결] BookAuthor 저장
        // ==========================================
        List<BookAuthor> bookAuthors = new ArrayList<>();

        // savedBooks와 targetDtos는 인덱스 순서가 같음
        for (int i = 0; i < savedBooks.size(); i++) {
            Book book = savedBooks.get(i);
            ParsingDto dto = targetDtos.get(i);

            if (dto.getAuthors() == null)
                continue;

            for (String authorName : dto.getAuthors()) {
                Author author = authorMap.get(authorName);

                if (author != null) {
                    bookAuthors.add(BookAuthor.builder()
                        .book(book)
                        .author(author)
                        .build());
                }
            }
        }

        // 5-1. 연결 테이블 한방 저장
        bookAuthorRepository.saveAll(bookAuthors);
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
