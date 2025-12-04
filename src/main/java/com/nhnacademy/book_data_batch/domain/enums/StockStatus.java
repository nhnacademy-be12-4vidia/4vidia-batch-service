package com.nhnacademy.book_data_batch.domain.enums;

import lombok.Getter;

@Getter
public enum StockStatus {

    PRE_ORDER(0), IN_STOCK(1), OUT_OF_STOCK(2), OUT_OF_PRINT(3);

    private final int code;

    StockStatus(int code) {
        this.code = code;
    }

    public static StockStatus of(int code) {
        for (StockStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        } throw new IllegalArgumentException("일치하는 재고 상태코드가 없습니다: " + code);
    }
}
