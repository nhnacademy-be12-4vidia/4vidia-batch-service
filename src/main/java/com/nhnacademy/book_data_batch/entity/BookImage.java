package com.nhnacademy.book_data_batch.entity;

import com.nhnacademy.book_data_batch.entity.converters.ImageTypeConverter;
import com.nhnacademy.book_data_batch.entity.enums.ImageType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "book_image", indexes = {
    @Index(name = "idx_book_image_book_id", columnList = "book_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_image_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "image_type", nullable = false)
    @Convert(converter = ImageTypeConverter.class)
    private ImageType imageType;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Builder
    public BookImage(Book book, String imageUrl, ImageType imageType, Integer displayOrder) {
        this.book = book;
        this.imageUrl = imageUrl;
        this.imageType = imageType;
        this.displayOrder = displayOrder;
    }
}