package com.nhnacademy.book_data_batch.dto.aladin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class AladinItemDto {

    private String title;
    private String author;
    private String pubDate;
    private String description;
    private String isbn13;
    private Integer priceSales;
    private Integer priceStandard;
    private String cover;
    private String categoryName;
    private String publisher;

}
