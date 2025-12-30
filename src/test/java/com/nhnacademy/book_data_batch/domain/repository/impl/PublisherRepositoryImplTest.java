package com.nhnacademy.book_data_batch.domain.repository.impl;

import com.nhnacademy.book_data_batch.domain.entity.Publisher;
import com.nhnacademy.book_data_batch.domain.repository.impl.PublisherRepositoryImpl;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.domain.repository.PublisherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JdbcExecutor.class, PublisherRepositoryImpl.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("PublisherRepositoryImpl 통합 테스트")
class PublisherRepositoryImplTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PublisherRepository publisherRepository;

    @BeforeEach
    void setUp() {
        publisherRepository.deleteAll();
    }

    @Test
    @DisplayName("bulkInsert: 빈 셋일 때 아무것도 실행되지 않음")
    void bulkInsert_emptySet_noExecution() {
        Set<String> publisherNames = Set.of();

        publisherRepository.bulkInsert(publisherNames);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM publisher", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkInsert: 여러 출판사 삽입")
    void bulkInsert_multiplePublishers_insertsCorrectly() {
        Set<String> publisherNames = Set.of("NHN Books", "Kakao Books", "Naver Books");

        publisherRepository.bulkInsert(publisherNames);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM publisher", Integer.class);
        assertThat(count).isEqualTo(3);

        List<String> names = jdbcTemplate.queryForList("SELECT publisher_name FROM publisher ORDER BY publisher_name", String.class);
        assertThat(names).containsExactlyInAnyOrder("NHN Books", "Kakao Books", "Naver Books");
    }

    @Test
    @DisplayName("bulkInsert: 중복 출판사는 INSERT IGNORE로 무시됨")
    void bulkInsert_duplicatePublishers_ignored() {
        Publisher existingPublisher = new Publisher("NHN Books");
        publisherRepository.save(existingPublisher);

        Set<String> publisherNames = Set.of("NHN Books", "Kakao Books");

        publisherRepository.bulkInsert(publisherNames);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM publisher", Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("bulkInsert: 단일 출판사 삽입")
    void bulkInsert_singlePublisher_insertsCorrectly() {
        Set<String> publisherNames = Set.of("Sunny Books");

        publisherRepository.bulkInsert(publisherNames);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM publisher", Integer.class);
        assertThat(count).isEqualTo(1);

        String name = jdbcTemplate.queryForObject("SELECT publisher_name FROM publisher", String.class);
        assertThat(name).isEqualTo("Sunny Books");
    }
}
