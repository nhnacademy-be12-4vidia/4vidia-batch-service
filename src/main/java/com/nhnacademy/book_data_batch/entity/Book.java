package com.nhnacademy.book_data_batch.entity;

import com.nhnacademy.book_data_batch.entity.converters.StockStatusConverter;
import com.nhnacademy.book_data_batch.entity.enums.StockStatus;
import jakarta.persistence.*;

import java.time.LocalDate;

import lombok.*;

@Entity
@Table(name = "book", indexes = {
    @Index(name = "idx_book_publisher_id", columnList = "publisher_id"),
    @Index(name = "idx_book_category_id", columnList = "category_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Book extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long id;

    @Column(name = "isbn_13", unique = true, length = 13)
    private String isbn;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "subtitle", length = 500)
    private String subtitle;

    @Column(name = "book_index", columnDefinition = "TEXT")
    private String bookIndex;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id")
    private Publisher publisher;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "language", length = 10)
    private String language;

    @Column(name = "price_standard")
    private Integer priceStandard;

    @Column(name = "price_sales")
    private Integer priceSales;

    @Column(name = "stock", columnDefinition = "INT DEFAULT 10")
    private Integer stock = 10;

    @Column(name = "stock_status", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    @Convert(converter = StockStatusConverter.class)
    private StockStatus stockStatus = StockStatus.IN_STOCK;

    @Column(name = "packaging_available", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean packagingAvailable = true;

    @Column(name = "volume_number", columnDefinition = "INT DEFAULT 1")
    private Integer volumeNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Builder
    public Book(String isbn,
                String title,
                String description,
                String subtitle,
                String bookIndex,
                Publisher publisher,
                LocalDate publishedDate,
                Integer pageCount,
                String language,
                Integer priceStandard,
                Integer priceSales,
                Integer volumeNumber,
                Category category) {
        this.isbn = isbn;
        this.title = title;
        this.subtitle = subtitle;
        this.bookIndex = bookIndex;
        this.description = description;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.pageCount = pageCount;
        this.language = language;
        this.priceStandard = priceStandard;
        this.priceSales = priceSales;
        this.volumeNumber = volumeNumber;
        this.category = category;
    }
}
