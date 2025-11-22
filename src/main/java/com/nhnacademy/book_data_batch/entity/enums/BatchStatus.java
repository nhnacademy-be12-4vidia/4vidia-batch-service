package com.nhnacademy.book_data_batch.entity.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum BatchStatus {

    PENDING(0), IN_PROGRESS(1), COMPLETED(2), FAILED(3);

    private final int code;

    BatchStatus(int code) {
        this.code = code;
    }

    public static BatchStatus of(int code) {
        for (BatchStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        } throw new IllegalArgumentException("일치하는 배치 상태코드가 없습니다: " + code);
    }
}
