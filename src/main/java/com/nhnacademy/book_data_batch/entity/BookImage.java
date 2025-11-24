package com.nhnacademy.book_data_batch.entity;

import com.nhnacademy.book_data_batch.entity.enums.ImageType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "image_type", nullable = false)
    private ImageType imageType;

    @Builder
    public BookImage(Book book, String imageUrl, ImageType imageType) {
        this.book = book;
        this.imageUrl = imageUrl;
        this.imageType = imageType;
    }
}