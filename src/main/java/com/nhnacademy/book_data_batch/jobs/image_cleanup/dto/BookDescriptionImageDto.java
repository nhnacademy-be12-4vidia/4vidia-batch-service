package com.nhnacademy.book_data_batch.jobs.image_cleanup.dto;

import java.time.LocalDateTime;

public record BookDescriptionImageDto(
    Long id,
    String imageUrl,
    LocalDateTime createdAt
) {}