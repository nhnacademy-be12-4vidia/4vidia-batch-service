package com.nhnacademy.book_data_batch.jobs.image_cleanup.processor;

import com.nhnacademy.book_data_batch.domain.repository.BookRepository;
import com.nhnacademy.book_data_batch.jobs.image_cleanup.dto.BookDescriptionImageDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class ContentImageCleanupProcessor implements ItemProcessor<BookDescriptionImageDto, BookDescriptionImageDto> {

    private final BookRepository bookRepository;
    private List<String> recentDescriptions;

    @PostConstruct
    public void init() {
        // 1. 메모리 최적화: 최근 수정된 도서의 Description을 미리 로딩
        // (배치 시작 기준 24시간 이내 수정된 책들에는, 24시간 전 업로드된 이미지가 있을 수 있음)
        // 안전하게 48시간(2일) 전 수정본까지 로딩
        LocalDate twoDaysAgo = LocalDate.now().minusDays(2);
        this.recentDescriptions = bookRepository.findDescriptionsByUpdatedAtAfter(twoDaysAgo);
        log.info("Pre-loaded {} book descriptions for optimization.", recentDescriptions.size());
    }

    @Override
    public BookDescriptionImageDto process(BookDescriptionImageDto item) {
        String imageUrl = item.imageUrl();

        // 2. 메모리 상에서 매칭
        // Description 리스트를 순회하며 해당 URL이 포함되어 있는지 확인
        boolean matchRaw = recentDescriptions.stream()
                .anyMatch(desc -> desc.contains(imageUrl));

        boolean matchEncoded = false;
        if (!matchRaw) {
            try {
                String encodedUrl = URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);
                matchEncoded = recentDescriptions.stream()
                        .anyMatch(desc -> desc.contains(encodedUrl));
            } catch (Exception e) {
                log.warn("URL Encoding failed for: {}", imageUrl, e);
            }
        }

        if (matchRaw || matchEncoded) {
            // 사용 중이면 Writer로 넘기지 않음 (null 반환 = Filter)
            return null;
        }

        // 미사용이면 삭제 대상이므로 Writer로 전달
        log.info("Deleting unused image: {}", imageUrl);
        return item;
    }
}
