package com.nhnacademy.book_data_batch.batch.aladin.handler;

import com.nhnacademy.book_data_batch.batch.aladin.dto.BookAuthorDto;
import com.nhnacademy.book_data_batch.batch.aladin.dto.EnrichmentResultDto;
import com.nhnacademy.book_data_batch.batch.aladin.dto.EnrichmentResultDto.AuthorWithRole;
import com.nhnacademy.book_data_batch.entity.Author;
import com.nhnacademy.book_data_batch.repository.AuthorRepository;
import com.nhnacademy.book_data_batch.repository.BookAuthorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Author 및 BookAuthor 저장을 담당하는 Handler
 */
@Component
@RequiredArgsConstructor
public class AuthorSaveHandler implements EnrichmentSaveHandler {

    private final AuthorRepository authorRepository;
    private final BookAuthorRepository bookAuthorRepository;

    @Override
    public void handle(List<EnrichmentResultDto> items) {
        // TODO: 알라딘에서 authors를 그렇게 많이 안 주네... 아.... 일단... 보류.... 아... 그냥 어지럽네...
//        List<EnrichmentResultDto> successItems = filterSuccess(items);
//        if (successItems.isEmpty()) {
//            return;
//        }
//
//        // 모든 저자 이름 수집
//        Set<String> allAuthorNames = collectAuthorNames(successItems);
//        if (allAuthorNames.isEmpty()) {
//            return;
//        }
//
//        // 1. Author bulk insert
//        authorRepository.bulkInsert(allAuthorNames);
//
//        // 2. Author 캐시 로드
//        Map<String, Author> authorCache = loadAuthorCache(allAuthorNames);
//
//        // 3. BookAuthor 연결
//        List<BookAuthorDto> bookAuthorDtos = buildBookAuthorDtos(successItems, authorCache);
//        if (!bookAuthorDtos.isEmpty()) {
//            bookAuthorRepository.bulkInsert(bookAuthorDtos);
//        }
    }

    @Override
    public int getOrder() {
        return 30; // Image(20) 이후
    }

    private List<EnrichmentResultDto> filterSuccess(List<EnrichmentResultDto> items) {
        return items.stream()
                .filter(EnrichmentResultDto::isSuccess)
                .toList();
    }

    private Set<String> collectAuthorNames(List<EnrichmentResultDto> items) {
        return items.stream()
                .flatMap(r -> r.authors().stream())
                .map(AuthorWithRole::name)
                .collect(Collectors.toSet());
    }

    private Map<String, Author> loadAuthorCache(Set<String> authorNames) {
        return authorRepository.findAllByNameIn(authorNames)
                .stream()
                .collect(Collectors.toMap(Author::getName, Function.identity()));
    }

    private List<BookAuthorDto> buildBookAuthorDtos(
            List<EnrichmentResultDto> items, 
            Map<String, Author> authorCache) {
        
        List<BookAuthorDto> result = new ArrayList<>();
        
        for (EnrichmentResultDto item : items) {
            Long bookId = item.bookId();  // bookId 직접 사용
            
            for (AuthorWithRole authorWithRole : item.authors()) {
                Author author = authorCache.get(authorWithRole.name());
                if (author != null) {
                    result.add(new BookAuthorDto(bookId, author.getId(), authorWithRole.role()));
                }
            }
        }
        
        return result;
    }
}
