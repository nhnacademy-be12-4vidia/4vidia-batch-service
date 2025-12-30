package com.nhnacademy.book_data_batch.domain.repository.impl;

import com.nhnacademy.book_data_batch.domain.repository.AuthorRepository;
import com.nhnacademy.book_data_batch.domain.repository.impl.BookAuthorRepositoryImpl;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.BookAuthorDto;
import com.nhnacademy.book_data_batch.domain.entity.Author;
import com.nhnacademy.book_data_batch.domain.entity.Book;
import com.nhnacademy.book_data_batch.domain.entity.BookAuthor;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.domain.repository.BookAuthorRepository;
import com.nhnacademy.book_data_batch.domain.repository.BookRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JdbcExecutor.class, BookAuthorRepositoryImpl.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("BookAuthorRepositoryImpl 통합 테스트")
class BookAuthorRepositoryImplTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private BookAuthorRepository bookAuthorRepository;

    private Book createBook(String isbn, String title) {
        Book book = Book.builder()
                .isbn(isbn)
                .title(title)
                .volumeNumber(1)
                .build();
        return bookRepository.save(book);
    }

    private Author createAuthor(String name) {
        Author author = new Author(name);
        return authorRepository.save(author);
    }

    @BeforeEach
    void setUp() {
        bookAuthorRepository.deleteAll();
        bookRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    @DisplayName("bulkInsert: 빈 리스트일 때 아무것도 실행되지 않음")
    void bulkInsert_emptyList_noExecution() {
        List<BookAuthorDto> bookAuthors = List.of();

        bookAuthorRepository.bulkInsert(bookAuthors);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book_author", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkInsert: 여러 BookAuthor 레코드 삽입")
    void bulkInsert_multipleBookAuthors_insertsCorrectly() {
        Book book1 = createBook("1234567890123", "Book 1");
        Book book2 = createBook("1234567890124", "Book 2");
        Book book3 = createBook("1234567890125", "Book 3");

        Author author1 = createAuthor("Author 1");
        Author author2 = createAuthor("Author 2");

        List<BookAuthorDto> bookAuthors = List.of(
                new BookAuthorDto(book1.getId(), author1.getId(), "지은이"),
                new BookAuthorDto(book2.getId(), author1.getId(), "옮긴이"),
                new BookAuthorDto(book3.getId(), author2.getId(), "지은이")
        );

        bookAuthorRepository.bulkInsert(bookAuthors);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book_author", Integer.class);
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("bulkInsert: 중복된 book_id와 author_id 조합은 INSERT IGNORE로 무시됨")
    void bulkInsert_duplicateCombination_ignored() {
        Book book = createBook("1234567890123", "Book 1");
        Author author = createAuthor("Author 1");

        BookAuthor existingBookAuthor = new BookAuthor(book, author, "지은이");
        bookAuthorRepository.save(existingBookAuthor);

        List<BookAuthorDto> bookAuthors = List.of(
                new BookAuthorDto(book.getId(), author.getId(), "옮긴이")
        );

        bookAuthorRepository.bulkInsert(bookAuthors);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book_author", Integer.class);
        assertThat(count).isEqualTo(1);

        String role = jdbcTemplate.queryForObject("SELECT author_role FROM book_author WHERE book_id = ? AND author_id = ?", String.class, book.getId(), author.getId());
        assertThat(role).isEqualTo("지은이");
    }

    @Test
    @DisplayName("bulkInsert: 단일 BookAuthor 삽입")
    void bulkInsert_singleBookAuthor_insertsCorrectly() {
        Book book = createBook("1234567890123", "Book 1");
        Author author = createAuthor("Author 1");

        List<BookAuthorDto> bookAuthors = List.of(
                new BookAuthorDto(book.getId(), author.getId(), "지은이")
        );

        bookAuthorRepository.bulkInsert(bookAuthors);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book_author", Integer.class);
        assertThat(count).isEqualTo(1);

        String role = jdbcTemplate.queryForObject("SELECT author_role FROM book_author", String.class);
        assertThat(role).isEqualTo("지은이");
    }

    @Test
    @DisplayName("bulkInsert: null 역할 처리")
    void bulkInsert_nullRole_insertsNull() {
        Book book = createBook("1234567890123", "Book 1");
        Author author = createAuthor("Author 1");

        List<BookAuthorDto> bookAuthors = List.of(
                new BookAuthorDto(book.getId(), author.getId(), null)
        );

        bookAuthorRepository.bulkInsert(bookAuthors);

        String role = jdbcTemplate.queryForObject("SELECT author_role FROM book_author", String.class);
        assertThat(role).isNull();
    }
}
