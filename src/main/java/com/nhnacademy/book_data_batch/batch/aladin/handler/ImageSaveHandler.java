package com.nhnacademy.book_data_batch.batch.aladin.handler;

import com.nhnacademy.book_data_batch.batch.aladin.dto.EnrichmentResultDto;
import com.nhnacademy.book_data_batch.batch.book.dto.BookImageDto;
import com.nhnacademy.book_data_batch.entity.enums.ImageType;
import com.nhnacademy.book_data_batch.repository.BookImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * BookImage 저장을 담당하는 Handler
 * - Aladin API에서 받은 cover URL을 BookImage 테이블에 저장
 * - 이미지 타입: THUMBNAIL (Aladin cover는 썸네일)
 */
@Component
@RequiredArgsConstructor
public class ImageSaveHandler implements EnrichmentSaveHandler {

    private final BookImageRepository bookImageRepository;

    @Override
    public void handle(List<EnrichmentResultDto> items) {
        List<BookImageDto> imageDtos = items.stream()
                .filter(EnrichmentResultDto::isSuccess)
                .filter(EnrichmentResultDto::hasCoverUrl)
                .map(item -> new BookImageDto(
                        item.bookId(),
                        item.coverUrl(),
                        ImageType.THUMBNAIL.getCode()
                ))
                .toList();

        if (imageDtos.isEmpty()) {
            return;
        }

        bookImageRepository.bulkInsert(imageDtos);
    }

    @Override
    public int getOrder() {
        return 20;  // Book 업데이트(1) 후, Author(3) 전에 실행
    }
}
