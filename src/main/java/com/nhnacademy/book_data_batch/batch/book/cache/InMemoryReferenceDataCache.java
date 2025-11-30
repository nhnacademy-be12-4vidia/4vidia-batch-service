package com.nhnacademy.book_data_batch.batch.book.cache;

import com.nhnacademy.book_data_batch.entity.Author;
import com.nhnacademy.book_data_batch.entity.Publisher;
import com.nhnacademy.book_data_batch.repository.AuthorRepository;
import com.nhnacademy.book_data_batch.repository.PublisherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 참조 데이터 캐시 구현체 (In-Memory)
 * - Step 1 (ReferenceDataLoadTasklet): CSV 스캔 → Bulk INSERT → buildFromRepositories() 호출
 * - Step 2 (BookItemWriter): findAuthor(), findPublisher()로 캐시 조회 (멀티스레드 안전)
 * - Job 완료 후: 필요시 clear() 호출
 */
@Slf4j
@Component
public class InMemoryReferenceDataCache implements ReferenceDataCache {

    private final Map<String, Author> authorCache = new ConcurrentHashMap<>();
    private final Map<String, Publisher> publisherCache = new ConcurrentHashMap<>();
    private final AtomicBoolean ready = new AtomicBoolean(false);

    @Override
    public Author findAuthor(String authorName) {
        if (authorName == null || authorName.isBlank()) {
            return null;
        }
        return authorCache.get(normalizeKey(authorName));
    }

    @Override
    public Publisher findPublisher(String publisherName) {
        if (publisherName == null || publisherName.isBlank()) {
            return null;
        }
        return publisherCache.get(normalizeKey(publisherName));
    }

    @Override
    public void putAuthor(String name, Author author) {
        if (name != null && author != null) {
            authorCache.put(normalizeKey(name), author);
        }
    }

    @Override
    public void putPublisher(String name, Publisher publisher) {
        if (name != null && publisher != null) {
            publisherCache.put(normalizeKey(name), publisher);
        }
    }

    @Override
    public void buildFromRepositories(
            AuthorRepository authorRepository,
            PublisherRepository publisherRepository
    ) {
        log.info("[Cache] 캐시 구축 시작...");
        long startTime = System.currentTimeMillis();

        // Author 캐시 구축
        authorRepository.findAll().forEach(author -> {
            if (author.getName() != null) {
                authorCache.put(normalizeKey(author.getName()), author);
            }
        });
        log.info("[Cache] Author 캐시 구축 완료: {}개, {}ms",
                authorCache.size(), System.currentTimeMillis() - startTime);

        // Publisher 캐시 구축
        long pubStartTime = System.currentTimeMillis();
        publisherRepository.findAll().forEach(publisher -> {
            if (publisher.getName() != null) {
                publisherCache.put(normalizeKey(publisher.getName()), publisher);
            }
        });
        log.info("[Cache] Publisher 캐시 구축 완료: {}개, {}ms",
                publisherCache.size(), System.currentTimeMillis() - pubStartTime);

        ready.set(true);
        log.info("[Cache] 전체 캐시 구축 완료. 총 소요시간: {}ms", System.currentTimeMillis() - startTime);
    }

    @Override
    public void clear() {
        authorCache.clear();
        publisherCache.clear();
        ready.set(false);
        log.info("[Cache] 캐시 초기화 완료");
    }

    @Override
    public boolean isReady() {
        return ready.get();
    }

    @Override
    public int getAuthorCacheSize() {
        return authorCache.size();
    }

    @Override
    public int getPublisherCacheSize() {
        return publisherCache.size();
    }

    private String normalizeKey(String key) {
        return key.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
