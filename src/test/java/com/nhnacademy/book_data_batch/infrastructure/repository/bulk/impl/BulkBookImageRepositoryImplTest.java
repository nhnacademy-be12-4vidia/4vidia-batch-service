package com.nhnacademy.book_data_batch.infrastructure.repository.bulk.impl;

import com.nhnacademy.book_data_batch.batch.domain.book.dto.BookImageDto;
import com.nhnacademy.book_data_batch.domain.Book;
import com.nhnacademy.book_data_batch.domain.enums.ImageType;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.infrastructure.repository.BookImageRepository;
import com.nhnacademy.book_data_batch.infrastructure.repository.BookRepository;
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
@Import({JdbcExecutor.class, BulkBookImageRepositoryImpl.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("BulkBookImageRepositoryImpl 통합 테스트")
class BulkBookImageRepositoryImplTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookImageRepository bookImageRepository;

    @BeforeEach
    void setUp() {
        bookImageRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("bulkInsert: 빈 리스트일 때 아무것도 실행되지 않음")
    void bulkInsert_emptyList_noExecution() {
        List<BookImageDto> bookImages = new ArrayList<>();

        bookImageRepository.bulkInsert(bookImages);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book_image", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkInsert: 여러 BookImage 레코드 삽입")
    void bulkInsert_multipleBookImages_insertsCorrectly() {
        Book book1 = createBook("1234567890123", "Book 1");
        Book book2 = createBook("1234567890124", "Book 2");
        Book book3 = createBook("1234567890125", "Book 3");

        List<BookImageDto> bookImages = new ArrayList<>();
        bookImages.add(new BookImageDto(book1.getId(), "http://example.com/image1.jpg", ImageType.THUMBNAIL.getCode(), 1));
        bookImages.add(new BookImageDto(book1.getId(), "http://example.com/image2.jpg", ImageType.DETAIL.getCode(), 2));
        bookImages.add(new BookImageDto(book2.getId(), "http://example.com/image3.jpg", ImageType.THUMBNAIL.getCode(), 1));
        bookImages.add(new BookImageDto(book3.getId(), "http://example.com/image4.jpg", ImageType.DETAIL.getCode(), 2));

        bookImageRepository.bulkInsert(bookImages);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book_image", Integer.class);
        assertThat(count).isEqualTo(4);
    }

    @Test
    @DisplayName("bulkInsert: 단일 BookImage 삽입")
    void bulkInsert_singleBookImage_insertsCorrectly() {
        Book book = createBook("1234567890123", "Book 1");

        List<BookImageDto> bookImages = new ArrayList<>();
        bookImages.add(new BookImageDto(book.getId(), "http://example.com/image.jpg", ImageType.THUMBNAIL.getCode(), 1));

        bookImageRepository.bulkInsert(bookImages);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM book_image", Integer.class);
        assertThat(count).isEqualTo(1);

        String imageUrl = jdbcTemplate.queryForObject("SELECT image_url FROM book_image", String.class);
        assertThat(imageUrl).isEqualTo("http://example.com/image.jpg");
    }

    @Test
    @DisplayName("bulkInsert: imageType 코드 확인")
    void bulkInsert_imageTypeCode_correct() {
        Book book = createBook("1234567890123", "Book 1");

        List<BookImageDto> bookImages = new ArrayList<>();
        bookImages.add(new BookImageDto(book.getId(), "http://example.com/thumb.jpg", ImageType.THUMBNAIL.getCode(), 1));
        bookImages.add(new BookImageDto(book.getId(), "http://example.com/detail.jpg", ImageType.DETAIL.getCode(), 2));

        bookImageRepository.bulkInsert(bookImages);

        List<Integer> types = jdbcTemplate.queryForList("SELECT image_type FROM book_image ORDER BY display_order", Integer.class);
        assertThat(types).containsExactly(ImageType.THUMBNAIL.getCode(), ImageType.DETAIL.getCode());
    }

    @Test
    @DisplayName("bulkInsert: displayOrder 확인")
    void bulkInsert_displayOrder_correct() {
        Book book = createBook("1234567890123", "Book 1");

        List<BookImageDto> bookImages = new ArrayList<>();
        bookImages.add(new BookImageDto(book.getId(), "http://example.com/thumb.jpg", ImageType.THUMBNAIL.getCode(), 5));
        bookImages.add(new BookImageDto(book.getId(), "http://example.com/detail.jpg", ImageType.DETAIL.getCode(), 10));

        bookImageRepository.bulkInsert(bookImages);

        List<Integer> orders = jdbcTemplate.queryForList("SELECT display_order FROM book_image ORDER BY display_order", Integer.class);
        assertThat(orders).containsExactly(5, 10);
    }

    private Book createBook(String isbn, String title) {
        Book book = Book.builder()
                .isbn(isbn)
                .title(title)
                .volumeNumber(1)
                .build();
        return bookRepository.save(book);
    }
}
