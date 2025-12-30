package com.nhnacademy.book_data_batch.jobs.image_cleanup.reader;

import com.nhnacademy.book_data_batch.jobs.image_cleanup.dto.BookDescriptionImageDto;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.DataClassRowMapper;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class ContentImageCleanupReaderConfig {

    private static final int CHUNK_SIZE = 100;

    @Bean
    public JdbcPagingItemReader<BookDescriptionImageDto> cleanupReader(DataSource dataSource) throws Exception {
        // 24시간 지난 이미지 조회
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        return new JdbcPagingItemReaderBuilder<BookDescriptionImageDto>()
                .name("cleanupReader")
                .dataSource(dataSource)
                .fetchSize(CHUNK_SIZE)
                .rowMapper(new DataClassRowMapper<>(BookDescriptionImageDto.class))
                .queryProvider(cleanupQueryProvider(dataSource))
                .parameterValues(Map.of("oneDayAgo", oneDayAgo))
                .build();
    }

    private PagingQueryProvider cleanupQueryProvider(DataSource dataSource) throws Exception {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("id, image_url, created_at");
        queryProvider.setFromClause("from book_description_image");
        queryProvider.setWhereClause("created_at < :oneDayAgo");
        queryProvider.setSortKeys(Map.of("id", Order.ASCENDING));
        return queryProvider.getObject();
    }
}
