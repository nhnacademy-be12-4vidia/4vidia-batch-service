package com.nhnacademy.book_data_batch.jobs.discount_reprice.event;

import java.io.Serializable;
import java.time.LocalDateTime;

public record DiscountPolicyChangedEvent(
    Long categoryId,
    String eventType,
    LocalDateTime changedAt
) implements Serializable {}
