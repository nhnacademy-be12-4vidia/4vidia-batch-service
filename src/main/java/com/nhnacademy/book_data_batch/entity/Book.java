package com.nhnacademy.book_data_batch.entity;

import com.nhnacademy.book_data_batch.entity.converters.StockStatusConverter;
import com.nhnacademy.book_data_batch.entity.enums.ImageType;
import com.nhnacademy.book_data_batch.entity.enums.StockStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "book", indexes = {
    @Index(name = "idx_book_publisher_id", columnList = "publisher_id"),
    @Index(name = "idx_book_category_id", columnList = "category_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Book extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long id;

    @Column(name = "isbn_13", unique = true, length = 13)
    @Setter
    private String isbn13;

    @Column(name = "title", nullable = false, length = 255)
    @Setter
    private String title;

    @Column(name = "subtitle", length = 255)
    @Setter
    private String subtitle;

    @Column(name = "book_index", columnDefinition = "TEXT")
    @Setter
    private String bookIndex;

    @Column(name = "description", columnDefinition = "TEXT")
    @Setter
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    @Setter
    private Publisher publisher;

    @Column(name = "published_date")
    @Setter
    private LocalDate publishedDate;

    @Column(name = "page_count")
    @Setter
    private Integer pageCount;

    @Column(name = "language", length = 10)
    @Setter
    private String language;

    @Column(name = "price_standard")
    @Setter
    private Integer priceStandard;

    @Column(name = "price_sales")
    @Setter
    private Integer priceSales;

    @Column(name = "stock", columnDefinition = "INT DEFAULT 0")
    @Setter
    private Integer stock = 0;

    @Column(name = "stock_status", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    @Setter
    @Convert(converter = StockStatusConverter.class)
    private StockStatus stockStatus = StockStatus.PRE_ORDER;

    @Column(name = "packaging_available", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Setter
    private Boolean packagingAvailable = true;

    @Column(name = "volume_number", columnDefinition = "INT DEFAULT 1")
    @Setter
    private Integer volumeNumber = 1;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @Setter
    private Category category;

    @Builder
    public Book(String isbn13,
                String title,
                String description,
                String subtitle,
                String bookIndex,
                Publisher publisher,
                LocalDate publishedDate,
                Integer priceStandard,
                Integer priceSales,
                Integer volumeNumber) {
        this.isbn13 = isbn13;
        this.title = title;
        this.subtitle = subtitle;
        this.bookIndex = bookIndex;
        this.description = description;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.priceStandard = priceStandard;
        this.priceSales = priceSales;
        this.volumeNumber = volumeNumber;
    }
}
