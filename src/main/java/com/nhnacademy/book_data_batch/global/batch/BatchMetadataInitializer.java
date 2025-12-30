package com.nhnacademy.book_data_batch.global.batch;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchMetadataInitializer {

    private static final String BATCH_JOB_INSTANCE_TABLE = "BATCH_JOB_INSTANCE";

    private final DataSource dataSource;

    // 애플리케이션 시작 시점에 배치 메타데이터 테이블 존재 여부를 확인하고,
    // 없으면 Spring Batch 제공 스키마로 초기화
    @PostConstruct
    public void ensureMetadataTables() {

        try {
            if (metadataTablesExist()) {
                return;
            }
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("org/springframework/batch/core/schema-mysql.sql"));
            DatabasePopulatorUtils.execute(populator, dataSource);
            log.info("Spring Batch 메타데이터 테이블을 schema-mysql.sql 로 생성");

        } catch (SQLException e) {
            throw new IllegalStateException("Spring Batch 메타데이터 테이블 초기화 실패", e);
        }
    }

    // BATCH_JOB_INSTANCE 테이블 존재 여부 확인
    private boolean metadataTablesExist() throws SQLException {

        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getTables(connection.getCatalog(), null,
                BATCH_JOB_INSTANCE_TABLE, null)) {
                return rs.next();
            }
        }
    }
}
