package com.nhnacademy.book_data_batch.jobs.discount_reprice.reader;

import com.nhnacademy.book_data_batch.jobs.discount_reprice.dto.DiscountRepriceTarget;
import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import org.springframework.batch.item.database.JpaPagingItemReader;

public class DiscountRepriceItemReader extends JpaPagingItemReader<DiscountRepriceTarget> {

    public DiscountRepriceItemReader(
            EntityManagerFactory entityManagerFactory,
            String queryString,
            Map<String, Object> parameterValues,
            int pageSize
    ) {
        setEntityManagerFactory(entityManagerFactory);
        setQueryString(queryString);
        setParameterValues(parameterValues);
        setPageSize(pageSize);
        setName("discountRepriceItemReader");
    }
}