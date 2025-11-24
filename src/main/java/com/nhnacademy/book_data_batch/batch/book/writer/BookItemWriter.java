package com.nhnacademy.book_data_batch.batch.book.writer;

import com.nhnacademy.book_data_batch.batch.book.dto.AuthorRole;
import com.nhnacademy.book_data_batch.batch.book.dto.BookNormalizedItem;
import com.nhnacademy.book_data_batch.entity.Author;
import com.nhnacademy.book_data_batch.entity.Batch;
import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.entity.BookAuthor;
import com.nhnacademy.book_data_batch.entity.Category;
import com.nhnacademy.book_data_batch.entity.Publisher;
import com.nhnacademy.book_data_batch.repository.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 정규화된 도서 데이터를 실제 DB 엔티티로 저장하는 Writer 입니다.
 * - 출판사/저자/카테고리 캐시를 마련해 중복 저장을 방지합니다.
 * - 신규 도서만 선별하여 book, book_author, batch 테이블에 일괄 저장합니다.
 */
@Slf4j
@RequiredArgsConstructor
@Transactional
public class BookItemWriter implements ItemWriter<BookNormalizedItem> {

    private final AuthorRepository authorRepository;
    private final PublisherRepository publisherRepository;
    private final BookRepository bookRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final CategoryRepository categoryRepository;
    private final BookImageRepository bookImageRepository;
    private final BatchRepository batchRepository;

    @Override
    public void write(Chunk<? extends BookNormalizedItem> chunk) {
        if (chunk.isEmpty()) {
            return;
        }

        List<? extends BookNormalizedItem> items = chunk.getItems();
        Map<String, Publisher> publisherMap = preparePublishers(items);
        Map<String, Author> authorMap = prepareAuthors(items);
        Map<String, Category> categoryMap = prepareCategories(items);

        if (categoryMap.isEmpty()) {
            throw new IllegalStateException("KDC 카테고리가 존재하지 않습니다. 먼저 카테고리 배치를 실행해주세요.");
        }

        Set<String> existingIsbns = loadExistingIsbns(items);
        List<Book> booksToSave = new ArrayList<>();
        List<BookNormalizedItem> validItems = new ArrayList<>();

        for (BookNormalizedItem item : items) {
            if (existingIsbns.contains(item.isbn13())) {
                log.debug("이미 등록된 ISBN 이라 건너뜁니다. isbn={}", item.isbn13());
                continue;
            }

            Category category = categoryMap.get(item.kdcCode());
            if (category == null) {
                log.warn("카테고리 미존재로 건너뜁니다. isbn={}, kdc={}", item.isbn13(), item.kdcCode());
                continue;
            }

            String publisherKey = normalizePublisherKey(item.publisherName());
            Publisher publisher = publisherMap.get(publisherKey);
            if (publisher == null) {
                log.warn("출판사 매핑 실패로 건너뜁니다. isbn={}, publisher={}", item.isbn13(), item.publisherName());
                continue;
            }

            Book book = new Book();
            book.setIsbn13(item.isbn13());
            book.setTitle(item.title());
            book.setDescription(item.description());
            book.setPublisher(publisher);
            book.setPublishedDate(item.publishedDate());
            book.setPriceStandard(item.priceStandard());
            book.setPriceSales(item.priceStandard());
            book.setCategory(category);
            book.setVolumeNumber(item.volumeNumber());
            booksToSave.add(book);
            validItems.add(item);
        }

        if (booksToSave.isEmpty()) {
            log.info("저장할 신규 도서가 없습니다. 청크 크기={}", items.size());
            return;
        }

        List<Book> savedBooks = bookRepository.saveAll(booksToSave);
        Map<String, Book> savedBookMap = savedBooks.stream()
            .collect(Collectors.toMap(Book::getIsbn13, book -> book));

        List<BookAuthor> relations = buildBookAuthors(validItems, savedBookMap, authorMap);
        if (!relations.isEmpty()) {
            bookAuthorRepository.saveAll(relations);
        }

        List<Batch> batches = savedBooks.stream()
            .map(Batch::new)
            .toList();
        if (!batches.isEmpty()) {
            batchRepository.saveAll(batches);
        }

        log.info("도서 청크 저장 완료 - 신규 도서 {}권, 저자 연결 {}건", savedBooks.size(), relations.size());
    }

    private Map<String, Publisher> preparePublishers(List<? extends BookNormalizedItem> items) {
        Map<String, String> canonicalNames = extractCanonicalPublisherNames(items);
        if (canonicalNames.isEmpty()) {
            return Map.of();
        }

        List<Publisher> existing = publisherRepository.findAllByNameIn(new LinkedHashSet<>(canonicalNames.values()));
        Map<String, Publisher> map = existing.stream()
            .collect(Collectors.toMap(
                publisher -> normalizePublisherKey(publisher.getName()),
                publisher -> publisher,
                (left, right) -> left,
                LinkedHashMap::new
            ));

        List<Publisher> toCreate = canonicalNames.entrySet().stream()
            .filter(entry -> !map.containsKey(entry.getKey()))
            .map(entry -> Publisher.builder().name(entry.getValue()).build())
            .toList();

        if (!toCreate.isEmpty()) {
            publisherRepository.saveAll(toCreate);
            toCreate.forEach(publisher -> map.put(normalizePublisherKey(publisher.getName()), publisher));
        }

        return map;
    }

    private Map<String, Author> prepareAuthors(List<? extends BookNormalizedItem> items) {
        Map<String, String> canonicalNames = extractCanonicalAuthorNames(items);
        if (canonicalNames.isEmpty()) {
            return Map.of();
        }

        List<Author> existing = authorRepository.findAllByNameIn(new LinkedHashSet<>(canonicalNames.values()));
        Map<String, Author> map = existing.stream()
            .collect(Collectors.toMap(
                author -> normalizeAuthorKey(author.getName()),
                author -> author,
                (left, right) -> left,
                LinkedHashMap::new
            ));

        List<Author> toCreate = canonicalNames.entrySet().stream()
            .filter(entry -> !map.containsKey(entry.getKey()))
            .map(entry -> Author.builder().name(entry.getValue()).build())
            .toList();
        if (!toCreate.isEmpty()) {
            authorRepository.saveAll(toCreate);
            toCreate.forEach(author -> map.put(normalizeAuthorKey(author.getName()), author));
        }
        return map;
    }

    private Map<String, Category> prepareCategories(List<? extends BookNormalizedItem> items) {
        Set<String> codes = items.stream()
            .map(BookNormalizedItem::kdcCode)
            .filter(StringUtils::hasText)
            .collect(Collectors.toCollection(HashSet::new));

        if (codes.isEmpty()) {
            return Map.of();
        }
        List<Category> categories = categoryRepository.findAllByKdcCodeIn(codes);
        return categories.stream()
            .collect(Collectors.toMap(Category::getKdcCode, category -> category));
    }

    private Set<String> loadExistingIsbns(List<? extends BookNormalizedItem> items) {
        Set<String> candidates = items.stream()
            .map(BookNormalizedItem::isbn13)
            .collect(Collectors.toCollection(HashSet::new));
        if (candidates.isEmpty()) {
            return Collections.emptySet();
        }
        return bookRepository.findAllByIsbn13In(candidates).stream()
            .map(Book::getIsbn13)
            .collect(Collectors.toSet());
    }

    private List<BookAuthor> buildBookAuthors(List<BookNormalizedItem> validItems,
                                              Map<String, Book> savedBookMap,
                                              Map<String, Author> authorMap) {
        List<BookAuthor> relations = new ArrayList<>();
        for (BookNormalizedItem item : validItems) {
            Book book = savedBookMap.get(item.isbn13());
            if (book == null || CollectionUtils.isEmpty(item.authorRoles())) {
                continue;
            }
            for (AuthorRole authorRole : item.authorRoles()) {
                String authorKey = normalizeAuthorKey(authorRole.name());
                if (!StringUtils.hasText(authorKey)) {
                    continue;
                }
                Author author = authorMap.get(authorKey);
                if (author == null) {
                    continue;
                }
                relations.add(BookAuthor.builder()
                    .book(book)
                    .author(author)
                    .role(authorRole.role())
                    .build());
            }
        }
        return relations;
    }

    /**
     * 저자명도 동일한 정규화 키로 관리해 중복 저장을 예방합니다.
     */
    private Map<String, String> extractCanonicalAuthorNames(List<? extends BookNormalizedItem> items) {
        Map<String, String> canonical = new LinkedHashMap<>();
        for (BookNormalizedItem item : items) {
            if (CollectionUtils.isEmpty(item.authorRoles())) {
                continue;
            }
            for (AuthorRole authorRole : item.authorRoles()) {
                String rawName = authorRole.name();
                if (!StringUtils.hasText(rawName)) {
                    continue;
                }
                String key = normalizeAuthorKey(rawName);
                if (!StringUtils.hasText(key)) {
                    continue;
                }
                canonical.putIfAbsent(key, rawName.trim());
            }
        }
        return canonical;
    }

    private String normalizeAuthorKey(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        return name.trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }

    /**
     * 출판사명도 동일한 정규화 키로 관리해 중복 저장을 예방합니다.
     */
    private Map<String, String> extractCanonicalPublisherNames(List<? extends BookNormalizedItem> items) {
        Map<String, String> canonical = new LinkedHashMap<>();
        for (BookNormalizedItem item : items) {
            String rawName = item.publisherName();
            if (!StringUtils.hasText(rawName)) {
                continue;
            }
            String key = normalizePublisherKey(rawName);
            if (!StringUtils.hasText(key)) {
                continue;
            }
            canonical.putIfAbsent(key, rawName.trim());
        }
        return canonical;
    }

    private String normalizePublisherKey(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        return name.trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }
}
