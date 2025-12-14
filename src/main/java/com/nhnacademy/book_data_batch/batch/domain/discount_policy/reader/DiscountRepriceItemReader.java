package com.nhnacademy.book_data_batch.batch.domain.discount_policy.reader;

import com.nhnacademy.book_data_batch.batch.domain.discount_policy.dto.DiscountRepriceTarget;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;

import java.util.Map;

public class DiscountRepriceItemReader extends JpaPagingItemReader<DiscountRepriceTarget> {

    public DiscountRepriceItemReader(EntityManagerFactory emf,
                                     String queryString,
                                     Map<String, Object> params,
                                     int pageSize) {
        super();
        this.setName("discountRepriceItemReader");
        this.setEntityManagerFactory(emf);
        this.setQueryString(queryString);
        this.setParameterValues(params);
        this.setPageSize(pageSize);
        this.setSaveState(true);
    }

    public static DiscountRepriceItemReader build(EntityManagerFactory emf,
                                                  String pathPrefix,
                                                  int pageSize) {
        String q = "SELECT new com.nhnacademy.book_data_batch.batch.domain.discount_policy.dto.DiscountRepriceTarget(" +
                " b.id, b.priceStandard, b.priceSales, c.id " +
                ") FROM Book b JOIN b.category c " +
                "WHERE c.path LIKE :pathPrefix " +
                "AND b.category.id NOT IN :excludedCategoryIds";
        Map<String, Object> params = Map.of(
                "pathPrefix", pathPrefix + "%",
                "excludedCategoryIds", java.util.Collections.emptyList()
        );
        return new DiscountRepriceItemReader(emf, q, params, pageSize);
    }
}
