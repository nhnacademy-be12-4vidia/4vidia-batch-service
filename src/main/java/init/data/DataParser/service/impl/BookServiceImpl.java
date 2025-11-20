package init.data.DataParser.service.impl;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Book;
import init.data.DataParser.repository.BookRepository;
import init.data.DataParser.service.BookService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Override
    public Book create(ParsingDto dto) {
        if (exist(dto.getIsbn())) {
            throw new IllegalArgumentException("이미 등록된 도서입니다.");
        }

        Book book = new Book();
        book.setIsbn(dto.getIsbn());
        book.setTitle(dto.getTitle());
        book.setPriceStandard(dto.getPriceStandard());
        book.setDescription(dto.getDescription());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate publishDate = LocalDate.parse(dto.getPublishedDate(), formatter);
        book.setPublishedDate(publishDate);

        return bookRepository.save(book);
    }

    @Override
    public boolean exist(String isbn) {
        return bookRepository.existsByIsbn(isbn);
    }

    @Override
    public Book getBook(String isbn) {
        if (bookRepository.existsByIsbn(isbn)) {
            return bookRepository.findByIsbn(isbn);
        }
        return null;
    }

    @Override
    public Book createByApi(Book book) {
        if (exist(book.getIsbn())) {
            throw new IllegalArgumentException("이미 등록된 도서입니다.");
        }
        return bookRepository.save(book);
    }

    @Override
    public List<Book> createAll(List<Book> booksToSave) {
        if (!booksToSave.isEmpty()) {
            return bookRepository.saveAll(booksToSave);
        }
        return List.of();
    }
}
