package com.nhnacademy.book_data_batch.domain.repository.impl;

import com.nhnacademy.book_data_batch.domain.repository.impl.BookRepositoryImpl;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.EnrichmentSuccessDto;
import com.nhnacademy.book_data_batch.domain.entity.Book;
import com.nhnacademy.book_data_batch.domain.entity.Category;
import com.nhnacademy.book_data_batch.domain.entity.Publisher;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.domain.repository.BookRepository;
import com.nhnacademy.book_data_batch.domain.repository.CategoryRepository;
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

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JdbcExecutor.class, BookRepositoryImpl.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("BookRepositoryImpl 통합 테스트")
class BookRepositoryImplTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PublisherRepository publisherRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Publisher createPublisher(String name) {
        Publisher publisher = new Publisher(name);
        return publisherRepository.save(publisher);
    }

    private Category createCategory(String kdcCode, String name, String path, int depth) {
        Category category = new Category(null, kdcCode, name, path, depth);
        return categoryRepository.save(category);
    }

    private Book createBook(String isbn, String title, Publisher publisher, Category category) {
        Book book = new Book(isbn, title, null, null, null, publisher, null, null, null, null, null, null, 1, category);
        return bookRepository.save(book);
    }

    @BeforeEach
    void setUp() {
        bookRepository.deleteAll();
        publisherRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    @DisplayName("bulkInsert: 빈 리스트일 때 아무것도 실행되지 않음")
    void bulkInsert_emptyList_noExecution() {
        List<Book> books = List.of();

        bookRepository.bulkInsert(books);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkInsert: 여러 책 삽입")
    void bulkInsert_multipleBooks_insertsCorrectly() {
        Publisher publisher = createPublisher("Test Publisher");
        Category category = createCategory("100", "Test Category", "/1", 1);

        Book book1 = new Book("1234567890123", "Book 1", "Description 1", null, null, publisher, null, null, null, 10000, null, null, 1, category);
        Book book2 = new Book("1234567890124", "Book 2", "Description 2", null, null, publisher, null, null, null, 20000, null, null, 2, category);
        Book book3 = new Book("1234567890125", "Book 3", "Description 3", null, null, publisher, null, null, null, 30000, null, null, 3, category);

        bookRepository.bulkInsert(List.of(book1, book2, book3));

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book", Integer.class);
        assertThat(count).isEqualTo(3);

        List<Integer> stocks = jdbcTemplate.queryForList("SELECT stock FROM book", Integer.class);
        assertThat(stocks).allMatch(s -> s == 0);

        List<Integer> stockStatuses = jdbcTemplate.queryForList("SELECT stock_status FROM book", Integer.class);
        assertThat(stockStatuses).allMatch(s -> s == 0);

        List<Boolean> packagingAvailable = jdbcTemplate.queryForList("SELECT packaging_available FROM book", Boolean.class);
        assertThat(packagingAvailable).allMatch(p -> p == true);
    }

    @Test
    @DisplayName("bulkInsert: priceStandard이 null이면 priceSales도 null")
    void bulkInsert_nullPriceStandard_nullPriceSales() {
        Publisher publisher = createPublisher("Test Publisher");
        Category category = createCategory("100", "Test Category", "/1", 1);

        Book book = new Book("1234567890123", "Book 1", "Description", null, null, publisher, null, null, null, null, null, null, 1, category);

        bookRepository.bulkInsert(List.of(book));

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book", Integer.class);
        assertThat(count).isEqualTo(1);

        Integer priceSales = jdbcTemplate.queryForObject("SELECT price_sales FROM book WHERE isbn_13 = ?", Integer.class, "1234567890123");
        assertThat(priceSales).isNull();
    }

    @Test
    @DisplayName("bulkInsert: priceStandard이 있으면 10% 할인된 판매가로 저장")
    void bulkInsert_withPriceStandard_calculatesPriceSales() {
        Publisher publisher = createPublisher("Test Publisher");
        Category category = createCategory("100", "Test Category", "/1", 1);

        Book book = new Book("1234567890123", "Book 1", "Description", null, null, publisher, null, null, null, 10000, null, null, 1, category);

        bookRepository.bulkInsert(List.of(book));

        Integer priceSales = jdbcTemplate.queryForObject("SELECT price_sales FROM book WHERE isbn_13 = ?", Integer.class, "1234567890123");
        assertThat(priceSales).isEqualTo(9000);
    }

    @Test
    @DisplayName("bulkInsert: priceSales가 있으면 그 값을 유지")
    void bulkInsert_withPriceSales_preservesValue() {
        Publisher publisher = createPublisher("Test Publisher");
        Category category = createCategory("100", "Test Category", "/1", 1);

        // priceStandard=10000, priceSales=8000 (not 9000)
        Book book = new Book("1234567890123", "Book 1", "Description", null, null, publisher, null, null, null, 10000, 8000, null, 1, category);

        bookRepository.bulkInsert(List.of(book));

        Integer priceSales = jdbcTemplate.queryForObject("SELECT price_sales FROM book WHERE isbn_13 = ?", Integer.class, "1234567890123");
        assertThat(priceSales).isEqualTo(8000);
    }

    @Test
    @DisplayName("bulkInsert: 중복 ISBN은 INSERT IGNORE로 무시됨")
    void bulkInsert_duplicateIsbn_ignored() {
        Publisher publisher = createPublisher("Test Publisher");
        Category category = createCategory("100", "Test Category", "/1", 1);

        createBook("1234567890123", "Existing Book", publisher, category);

        Book newBook = new Book("1234567890123", "New Book", "Description", null, null, publisher, null, null, null, 10000, null, null, 1, category);

        bookRepository.bulkInsert(List.of(newBook));

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book", Integer.class);
        assertThat(count).isEqualTo(1);

        String title = jdbcTemplate.queryForObject("SELECT title FROM book WHERE isbn_13 = ?", String.class, "1234567890123");
        assertThat(title).isEqualTo("Existing Book");
    }

    @Test
    @DisplayName("bulkUpdateFromEnrichment: 빈 리스트일 때 아무것도 실행되지 않음")
    void bulkUpdateFromEnrichment_emptyList_noExecution() {
        List<EnrichmentSuccessDto> enrichmentData = List.of();

        bookRepository.bulkUpdateFromEnrichment(enrichmentData);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkUpdateFromEnrichment: 필드 업데이트")
    void bulkUpdateFromEnrichment_updatesFields() {
        Publisher publisher = createPublisher("Test Publisher");
        Category category = createCategory("100", "Test Category", "/1", 1);

        Book book = createBook("1234567890123", "Book 1", publisher, category);

        EnrichmentSuccessDto enrichmentData = new EnrichmentSuccessDto(
                book.getId(),
                book.getId(),
                "Updated Description",
                20000,
                LocalDate.of(2025, 1, 1),
                "Updated Subtitle",
                500,
                "Updated Index",
                List.of(),
                List.of(),
                null,
                "ko"
        );

        bookRepository.bulkUpdateFromEnrichment(List.of(enrichmentData));

        String description = jdbcTemplate.queryForObject("SELECT description FROM book WHERE book_id = ?", String.class, book.getId());
        String subtitle = jdbcTemplate.queryForObject("SELECT subtitle FROM book WHERE book_id = ?", String.class, book.getId());
        String bookIndex = jdbcTemplate.queryForObject("SELECT book_index FROM book WHERE book_id = ?", String.class, book.getId());
        Integer pageCount = jdbcTemplate.queryForObject("SELECT page_count FROM book WHERE book_id = ?", Integer.class, book.getId());
        Integer priceSales = jdbcTemplate.queryForObject("SELECT price_sales FROM book WHERE book_id = ?", Integer.class, book.getId());
        String language = jdbcTemplate.queryForObject("SELECT language FROM book WHERE book_id = ?", String.class, book.getId());

        assertThat(description).isEqualTo("Updated Description");
        assertThat(subtitle).isEqualTo("Updated Subtitle");
        assertThat(bookIndex).isEqualTo("Updated Index");
        assertThat(pageCount).isEqualTo(500);
        assertThat(priceSales).isEqualTo(18000); // 20000 * 0.9
        assertThat(language).isEqualTo("ko");
    }

    @Test
    @DisplayName("bulkUpdateFromEnrichment: 여러 책 한 번에 업데이트")
    void bulkUpdateFromEnrichment_multipleBooks_updatesAll() {
        Publisher publisher = createPublisher("Test Publisher");
        Category category = createCategory("100", "Test Category", "/1", 1);

        Book book1 = createBook("1234567890123", "Book 1", publisher, category);
        Book book2 = createBook("1234567890124", "Book 2", publisher, category);

        List<EnrichmentSuccessDto> enrichmentData = Arrays.asList(
                new EnrichmentSuccessDto(book1.getId(), book1.getId(), "Desc 1", 10000, LocalDate.now(), "Sub 1", 100, "Idx 1", List.of(), List.of(), null, "ko"),
                new EnrichmentSuccessDto(book2.getId(), book2.getId(), "Desc 2", 20000, LocalDate.now(), "Sub 2", 200, "Idx 2", List.of(), List.of(), null, "en")
        );

        bookRepository.bulkUpdateFromEnrichment(enrichmentData);

        List<String> descriptions = jdbcTemplate.queryForList("SELECT description FROM book ORDER BY book_id", String.class);
        assertThat(descriptions).containsExactly("Desc 1", "Desc 2");
    }
}
