package com.nhnacademy.book_data_batch.batch.domain.book_image.dto.event;

import java.time.LocalDateTime;

public record DescriptionImageUploadedEvent(
        String imageUrl,
        LocalDateTime uploadedAt
) {
}
