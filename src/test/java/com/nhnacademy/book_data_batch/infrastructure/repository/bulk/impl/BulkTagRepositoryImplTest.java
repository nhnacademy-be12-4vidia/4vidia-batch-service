package com.nhnacademy.book_data_batch.infrastructure.repository.bulk.impl;

import com.nhnacademy.book_data_batch.domain.Tag;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.infrastructure.repository.TagRepository;
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
@Import({JdbcExecutor.class, BulkTagRepositoryImpl.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("BulkTagRepositoryImpl 통합 테스트")
class BulkTagRepositoryImplTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TagRepository tagRepository;

    @BeforeEach
    void setUp() {
        tagRepository.deleteAll();
    }

    @Test
    @DisplayName("bulkInsert: 빈 셋일 때 아무것도 실행되지 않음")
    void bulkInsert_emptySet_noExecution() {
        Set<String> tagNames = Set.of();

        tagRepository.bulkInsert(tagNames);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tag", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkInsert: 여러 태그 삽입")
    void bulkInsert_multipleTags_insertsCorrectly() {
        Set<String> tagNames = Set.of("Fantasy", "Romance", "Thriller");

        tagRepository.bulkInsert(tagNames);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tag", Integer.class);
        assertThat(count).isEqualTo(3);

        List<String> names = jdbcTemplate.queryForList("SELECT tag_name FROM tag ORDER BY tag_name", String.class);
        assertThat(names).containsExactlyInAnyOrder("Fantasy", "Romance", "Thriller");
    }

    @Test
    @DisplayName("bulkInsert: 중복 태그는 INSERT IGNORE로 무시됨")
    void bulkInsert_duplicateTags_ignored() {
        Tag existingTag = new Tag("Fantasy");
        tagRepository.save(existingTag);

        Set<String> tagNames = Set.of("Fantasy", "Romance");

        tagRepository.bulkInsert(tagNames);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tag", Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("bulkInsert: 단일 태그 삽입")
    void bulkInsert_singleTag_insertsCorrectly() {
        Set<String> tagNames = Set.of("Sci-Fi");

        tagRepository.bulkInsert(tagNames);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tag", Integer.class);
        assertThat(count).isEqualTo(1);

        String name = jdbcTemplate.queryForObject("SELECT tag_name FROM tag", String.class);
        assertThat(name).isEqualTo("Sci-Fi");
    }
}
