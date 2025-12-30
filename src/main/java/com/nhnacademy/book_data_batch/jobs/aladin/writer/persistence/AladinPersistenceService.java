package com.nhnacademy.book_data_batch.jobs.aladin.writer.persistence;

import com.nhnacademy.book_data_batch.domain.repository.*;
import com.nhnacademy.book_data_batch.jobs.book_import.dto.BookImageDto;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.BookAuthorDto;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.domain.enums.ImageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aladin API를 통해 보강된 데이터를 영속화(저장 및 업데이트)하는 서비스.
 * ItemWriter에서 호출되며, Batch 처리 단위로 트랜잭션을 관리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AladinPersistenceService {


    private final AuthorRepository authorRepository;
    private final BookAuthorRepository bookAuthorRepository;
    private final TagRepository tagRepository;
    private final BookTagRepository bookTagRepository;
    private final BookRepository bookRepository;
    private final BookImageRepository bookImageRepository;

    /**
     * 알라딘 API를 통해 보강된 도서 정보를 DB에 영속화합니다.
     * 저자, 태그, 책 정보, 책 이미지 등을 일괄 처리합니다.
     * 이 메소드 호출 전체가 하나의 트랜잭션으로 묶입니다.
     *
     * @param results 보강 성공 데이터 목록
     */
    @Transactional
    public void saveEnrichmentData(List<EnrichmentSuccessDto> results) {
        if (results.isEmpty()) {
            return;
        }

        saveAuthors(results);
        saveTags(results);
        saveBooks(results);
        saveImages(results);
        
        log.debug("[AladinPersistenceService] Saved enrichment data for {} books", results.size());
    }

    /**
     * Author 저장 및 BookAuthor 관계 생성
     */
    private void saveAuthors(List<EnrichmentSuccessDto> results) {
        // 1. 모든 저자 이름 수집 (중복 제거)
        Set<String> authorNames = results.stream()
                .filter(EnrichmentSuccessDto::hasAuthors)
                .flatMap(r -> r.authors().stream())
                .map(EnrichmentSuccessDto.AuthorWithRole::name)
                .collect(Collectors.toSet());

        if (authorNames.isEmpty()) {
            log.debug("[AladinPersistenceService] No authors to save.");
            return;
        }

        // 2. Author bulk insert (INSERT IGNORE)
        authorRepository.bulkInsert(authorNames);

        // 3. Author ID 조회 (JDBC로 직접 조회하여 MySQL 메타데이터 캐시 오류 회피)
        Map<String, Long> authorIdMap = authorRepository.findIdsByNames(authorNames, 500); // BatchSize 500 사용

        // 4. BookAuthor 관계 생성
        List<BookAuthorDto> bookAuthors = new ArrayList<>();
        for (EnrichmentSuccessDto data : results) {
            if (!data.hasAuthors()) continue;

            for (EnrichmentSuccessDto.AuthorWithRole author : data.authors()) {
                Long authorId = authorIdMap.get(author.name());
                if (authorId != null) {
                    bookAuthors.add(new BookAuthorDto(data.bookId(), authorId, author.role()));
                }
            }
        }

        // 5. BookAuthor bulk insert
        if (!bookAuthors.isEmpty()) {
            bookAuthorRepository.bulkInsert(bookAuthors);
        }
    }

    /**
     * Tag 저장 및 BookTag 관계 생성
     */
    private void saveTags(List<EnrichmentSuccessDto> results) {
        // 1. 모든 태그 이름 수집 (중복 제거)
        Set<String> tagNames = results.stream()
                .filter(EnrichmentSuccessDto::hasTags)
                .flatMap(r -> r.tags().stream())
                .collect(Collectors.toSet());

        if (tagNames.isEmpty()) {
            log.debug("[AladinPersistenceService] No tags to save.");
            return;
        }

        // 2. Tag bulk insert (INSERT IGNORE)
        tagRepository.bulkInsert(tagNames);

        // 3. Tag ID 조회
        Map<String, Long> tagIdMap = tagRepository.findIdsByNames(tagNames);

        // 4. BookTag 관계 생성
        List<long[]> bookTagPairs = new ArrayList<>();
        for (EnrichmentSuccessDto data : results) {
            if (!data.hasTags()) continue;

            for (String tagName : data.tags()) {
                Long tagId = tagIdMap.get(tagName);
                if (tagId != null) {
                    bookTagPairs.add(new long[]{data.bookId(), tagId});
                }
            }
        }

        // 5. BookTag bulk insert
        if (!bookTagPairs.isEmpty()) {
            bookTagRepository.bulkInsert(bookTagPairs);
        }
    }

    /**
     * Book 엔티티 업데이트 (Aladin 보강 정보 반영)
     */
    private void saveBooks(List<EnrichmentSuccessDto> results) {
        if (!results.isEmpty()) {
            bookRepository.bulkUpdateFromEnrichment(results);
        }
    }

    /**
     * BookImage 저장
     */
    private void saveImages(List<EnrichmentSuccessDto> results) {
        List<BookImageDto> images = results.stream()
                .filter(EnrichmentSuccessDto::hasCoverUrl)
                .map(r -> new BookImageDto(
                        r.bookId(),
                        r.coverUrl(),
                        ImageType.THUMBNAIL.getCode(),
                        0 // displayOrder
                ))
                .toList();
        if (!images.isEmpty()) {
            bookImageRepository.bulkInsert(images);
        }
    }
}
