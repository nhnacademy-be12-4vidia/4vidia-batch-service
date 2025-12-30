package com.nhnacademy.book_data_batch.jobs.image_cleanup.dto.event;

import java.time.LocalDateTime;

public record DescriptionImageUploadedEvent(
        String imageUrl,
        LocalDateTime uploadedAt
) {
}
