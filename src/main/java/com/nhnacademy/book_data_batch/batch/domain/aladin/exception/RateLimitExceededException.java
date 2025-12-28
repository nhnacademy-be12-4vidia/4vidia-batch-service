package com.nhnacademy.book_data_batch.batch.domain.aladin.exception;

import lombok.Getter;

/**
 * API Rate Limit (분당/시간당 제한) 초과 시 발생하는 예외
 * - 재시도 가능한 예외
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final String apiKey;
    private final int httpStatusCode;

    public RateLimitExceededException(String apiKey, int httpStatusCode, String message) {
        super(message);
        this.apiKey = apiKey;
        this.httpStatusCode = httpStatusCode;
    }
}
