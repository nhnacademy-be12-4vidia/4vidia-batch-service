package com.nhnacademy.book_data_batch.batch.domain.book.dto;

/**
 * 도서 이미지 DTO
 *
 * @param bookId    도서 ID (FK)
 * @param imageUrl  이미지 URL
 * @param imageType 이미지 타입 (0: THUMBNAIL, 1: DETAIL)
 */
public record BookImageDto(

        Long bookId,
        String imageUrl,
        int imageType,
        int displayOrder
) {

}
