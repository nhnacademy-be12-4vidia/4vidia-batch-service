package com.nhnacademy.book_data_batch.batch.enrichment.handler;

import com.nhnacademy.book_data_batch.batch.enrichment.dto.EnrichmentResultDto;
import com.nhnacademy.book_data_batch.entity.Tag;
import com.nhnacademy.book_data_batch.repository.BookTagRepository;
import com.nhnacademy.book_data_batch.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tag 및 BookTag 저장을 담당하는 Handler
 */
@Component
@RequiredArgsConstructor
public class TagSaveHandler implements EnrichmentSaveHandler {

    private final TagRepository tagRepository;
    private final BookTagRepository bookTagRepository;

    @Override
    public void handle(List<EnrichmentResultDto> items) {
        List<EnrichmentResultDto> successItems = filterSuccess(items);
        if (successItems.isEmpty()) {
            return;
        }

        // 모든 태그 이름 수집
        Set<String> allTagNames = collectTagNames(successItems);
        if (allTagNames.isEmpty()) {
            return;
        }

        // 1. Tag bulk insert
        tagRepository.bulkInsert(allTagNames);

        // 2. Tag 캐시 로드
        Map<String, Tag> tagCache = loadTagCache(allTagNames);

        // 3. BookTag 연결
        List<long[]> bookTagPairs = buildBookTagPairs(successItems, tagCache);
        if (!bookTagPairs.isEmpty()) {
            bookTagRepository.bulkInsert(bookTagPairs);
        }
    }

    @Override
    public int getOrder() {
        return 40; // Author(30) 이후
    }

    private List<EnrichmentResultDto> filterSuccess(List<EnrichmentResultDto> items) {
        return items.stream()
                .filter(EnrichmentResultDto::isSuccess)
                .toList();
    }

    private Set<String> collectTagNames(List<EnrichmentResultDto> items) {
        return items.stream()
                .flatMap(r -> r.tags().stream())
                .collect(Collectors.toSet());
    }

    private Map<String, Tag> loadTagCache(Set<String> tagNames) {
        return tagRepository.findAllByNameIn(tagNames)
                .stream()
                .collect(Collectors.toMap(Tag::getName, Function.identity()));
    }

    private List<long[]> buildBookTagPairs(
            List<EnrichmentResultDto> items, 
            Map<String, Tag> tagCache) {
        
        List<long[]> result = new ArrayList<>();
        
        for (EnrichmentResultDto item : items) {
            Long bookId = item.bookId();  // bookId 직접 사용
            
            for (String tagName : item.tags()) {
                Tag tag = tagCache.get(tagName);
                if (tag != null) {
                    result.add(new long[]{bookId, tag.getId()});
                }
            }
        }
        
        return result;
    }
}
