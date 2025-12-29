package com.nhnacademy.book_data_batch.infrastructure.repository.bulk.impl;

import com.nhnacademy.book_data_batch.domain.Author;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.infrastructure.repository.AuthorRepository;
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
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JdbcExecutor.class, BulkAuthorRepositoryImpl.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("BulkAuthorRepositoryImpl 통합 테스트")
class BulkAuthorRepositoryImplTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorRepository authorRepository;

    @BeforeEach
    void setUp() {
        authorRepository.deleteAll();
    }

    @Test
    @DisplayName("bulkInsert: 빈 셋일 때 아무것도 실행되지 않음")
    void bulkInsert_emptySet_noExecution() {
        Set<String> authorNames = Set.of();

        authorRepository.bulkInsert(authorNames);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM author", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkInsert: 여러 작가 삽입")
    void bulkInsert_multipleAuthors_insertsCorrectly() {
        Set<String> authorNames = Set.of("Author A", "Author B", "Author C");

        authorRepository.bulkInsert(authorNames);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM author", Integer.class);
        assertThat(count).isEqualTo(3);

        List<String> names = jdbcTemplate.queryForList("SELECT author_name FROM author ORDER BY author_name", String.class);
        assertThat(names).containsExactlyInAnyOrder("Author A", "Author B", "Author C");
    }

    @Test
    @DisplayName("bulkInsert: 중복 작가는 INSERT IGNORE로 무시됨")
    void bulkInsert_duplicateAuthors_ignored() {
        Author existingAuthor = new Author("Author A");
        authorRepository.save(existingAuthor);

        Set<String> authorNames = Set.of("Author A", "Author B");

        authorRepository.bulkInsert(authorNames);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM author", Integer.class);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("bulkInsert: 단일 작가 삽입")
    void bulkInsert_singleAuthor_insertsCorrectly() {
        Set<String> authorNames = Set.of("Solo Author");

        authorRepository.bulkInsert(authorNames);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM author", Integer.class);
        assertThat(count).isEqualTo(1);

        String name = jdbcTemplate.queryForObject("SELECT author_name FROM author", String.class);
        assertThat(name).isEqualTo("Solo Author");
    }

    @Test
    @DisplayName("findIdsByNames: 빈 셋일 때 빈 맵 반환")
    void findIdsByNames_emptySet_returnsEmptyMap() {
        Set<String> names = Set.of();

        Map<String, Long> result = authorRepository.findIdsByNames(names, 10);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findIdsByNames: 작가 이름으로 ID 맵 조회")
    void findIdsByNames_returnsCorrectMap() {
        Author author1 = new Author("Author A");
        Author author2 = new Author("Author B");
        author1 = authorRepository.save(author1);
        author2 = authorRepository.save(author2);

        Set<String> names = Set.of("Author A", "Author B");

        Map<String, Long> result = authorRepository.findIdsByNames(names, 10);

        assertThat(result).hasSize(2);
        assertThat(result.get("Author A")).isEqualTo(author1.getId());
        assertThat(result.get("Author B")).isEqualTo(author2.getId());
    }

    @Test
    @DisplayName("findIdsByNames: 존재하지 않는 작가는 맵에 포함되지 않음")
    void findIdsByNames_nonExistingAuthor_notInMap() {
        Author author = new Author("Existing Author");
        author = authorRepository.save(author);

        Set<String> names = Set.of("Existing Author", "Non-existing Author");

        Map<String, Long> result = authorRepository.findIdsByNames(names, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get("Existing Author")).isEqualTo(author.getId());
        assertThat(result.get("Non-existing Author")).isNull();
    }

    @Test
    @DisplayName("findIdsByNames: 배치 사이즈 테스트")
    void findIdsByNames_batchSizeTest() {
        Author author1 = new Author("A1");
        Author author2 = new Author("A2");
        Author author3 = new Author("A3");
        author1 = authorRepository.save(author1);
        author2 = authorRepository.save(author2);
        author3 = authorRepository.save(author3);

        Set<String> names = Set.of("A1", "A2", "A3");

        Map<String, Long> result = authorRepository.findIdsByNames(names, 2);

        assertThat(result).hasSize(3);
        assertThat(result.get("A1")).isEqualTo(author1.getId());
        assertThat(result.get("A2")).isEqualTo(author2.getId());
        assertThat(result.get("A3")).isEqualTo(author3.getId());
    }
}
