package com.nhnacademy.book_data_batch.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "book_description_image", indexes = {
    @Index(name = "idx_book_desc_img_created_at", columnList = "created_at"),
    @Index(name = "idx_book_desc_img_url", columnList = "image_url")
})
public class BookDescriptionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_url", nullable = false, length = 500, unique = true)
    private String imageUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public BookDescriptionImage(String imageUrl, LocalDateTime createdAt) {
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }
}
