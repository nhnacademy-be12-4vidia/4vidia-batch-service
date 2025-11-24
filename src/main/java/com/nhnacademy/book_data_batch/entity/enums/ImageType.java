package com.nhnacademy.book_data_batch.entity.enums;

import lombok.Getter;

@Getter
public enum ImageType {

    THUMBNAIL(0), DETAIL(1);

    private final int code;

    ImageType(int code) {
        this.code = code;
    }

    public static ImageType of(int code) {
        for (ImageType type : ImageType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("일치하는 이미지타입 코드가 없습니다. : %d".formatted(code));
    }
}
