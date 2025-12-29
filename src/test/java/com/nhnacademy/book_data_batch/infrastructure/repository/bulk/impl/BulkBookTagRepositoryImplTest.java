package com.nhnacademy.book_data_batch.infrastructure.repository.bulk.impl;

import com.nhnacademy.book_data_batch.domain.Book;
import com.nhnacademy.book_data_batch.domain.BookTag;
import com.nhnacademy.book_data_batch.domain.Tag;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.infrastructure.repository.BookRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.BookTagRepository;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JdbcExecutor.class, BulkBookTagRepositoryImpl.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("BulkBookTagRepositoryImpl 통합 테스트")
class BulkBookTagRepositoryImplTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private BookTagRepository bookTagRepository;

    private Book createBook(String isbn, String title) {
        Book book = Book.builder()
                .isbn(isbn)
                .title(title)
                .volumeNumber(1)
                .build();
        return bookRepository.save(book);
    }

    private Tag createTag(String name) {
        Tag tag = new Tag(name);
        return tagRepository.save(tag);
    }

    @BeforeEach
    void setUp() {
        bookTagRepository.deleteAll();
        bookRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    @DisplayName("bulkInsert: 빈 리스트일 때 아무것도 실행되지 않음")
    void bulkInsert_emptyList_noExecution() {
        List<long[]> bookTagPairs = new ArrayList<>();

        bookTagRepository.bulkInsert(bookTagPairs);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book_tag", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkInsert: 여러 BookTag 레코드 삽입")
    void bulkInsert_multipleBookTags_insertsCorrectly() {
        Book book1 = createBook("1234567890123", "Book 1");
        Book book2 = createBook("1234567890124", "Book 2");
        Book book3 = createBook("1234567890125", "Book 3");

        Tag tag1 = createTag("Tag 1");
        Tag tag2 = createTag("Tag 2");
        Tag tag3 = createTag("Tag 3");

        List<long[]> bookTagPairs = new ArrayList<>();
        bookTagPairs.add(new long[]{book1.getId(), tag1.getId()});
        bookTagPairs.add(new long[]{book1.getId(), tag2.getId()});
        bookTagPairs.add(new long[]{book2.getId(), tag3.getId()});
        bookTagPairs.add(new long[]{book3.getId(), tag1.getId()});

        bookTagRepository.bulkInsert(bookTagPairs);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book_tag", Integer.class);
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("bulkInsert: 단일 BookTag 삽입")
    void bulkInsert_singleBookTag_insertsCorrectly() {
        Book book = createBook("1234567890123", "Book 1");
        Tag tag = createTag("Tag 1");

        List<long[]> bookTagPairs = new ArrayList<>();
        bookTagPairs.add(new long[]{book.getId(), tag.getId()});

        bookTagRepository.bulkInsert(bookTagPairs);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book_tag", Integer.class);
        assertThat(count).isEqualTo(1);

        Long bookId = jdbcTemplate.queryForObject("SELECT book_id FROM book_tag", Long.class);
        Long tagId = jdbcTemplate.queryForObject("SELECT tag_id FROM book_tag", Long.class);
        assertThat(bookId).isEqualTo(book.getId());
        assertThat(tagId).isEqualTo(tag.getId());
    }

    @Test
    @DisplayName("bulkInsert: 중복된 book_id와 tag_id 조합은 INSERT IGNORE로 무시됨")
    void bulkInsert_duplicateCombination_ignored() {
        Book book = createBook("1234567890123", "Book 1");
        Tag tag = createTag("Tag 1");

        BookTag existingBookTag = new BookTag(book, tag);
        bookTagRepository.save(existingBookTag);

        List<long[]> bookTagPairs = new ArrayList<>();
        bookTagPairs.add(new long[]{book.getId(), tag.getId()});

        bookTagRepository.bulkInsert(bookTagPairs);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book_tag", Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
