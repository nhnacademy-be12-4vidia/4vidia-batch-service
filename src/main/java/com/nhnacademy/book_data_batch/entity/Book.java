package com.nhnacademy.book_data_batch.entity;

import com.nhnacademy.book_data_batch.entity.enums.StockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Book extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "ISBN", length = 13)
    @Setter
    private String isbn;

    @Column(name = "제목", nullable = false, length = 1000)
    @Setter
    private String title;

    @Column(name = "부제목")
    @Setter
    private String subtitle;

    @Column(name = "목차", columnDefinition = "TEXT")
    @Setter
    private String index;

    @Column(name = "설명", columnDefinition = "TEXT")
    @Setter
    private String description;

//    @Column(name = "이미지url", length = 500)
//    @Setter
//    private String imageUrl;

    @ManyToOne
    @Setter
    private Publisher publisher;

    @Column(name = "출판일시")
    @Setter
    private LocalDate publishedDate;

    @Column(name = "페이지수")
    @Setter
    private Integer pageCount;

    @Column(name = "언어", length = 10)
    @Setter
    private String language;

    @Column(name = "정가")
    @Setter
    private Integer priceStandard;

    @Column(name = "판매가")
    @Setter
    private Integer priceSales;

    @Column(name = "재고", columnDefinition = "INT DEFAULT 0")
    @Setter
    private Integer stock;

    @Column(name = "재고상태")
    @Setter
    @Enumerated(value = EnumType.STRING)
    private StockStatus stockStatus = StockStatus.OUT_OF_STOCK;

    @Column(name = "포장가능여부")
    @Setter
    private Boolean packagingAvailable = false;

    @Column(name = "총권수", columnDefinition = "INT DEFAULT 0")
    @Setter
    private Integer volumeNumber;

    @Builder
    public Book(String isbn, String title, String description,
        Publisher publisher,
        LocalDate publishedDate, Integer priceStandard, Integer priceSales) {
        this.isbn = isbn;
        this.title = title;
        this.description = description;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.priceSales = priceSales;
    }


}
