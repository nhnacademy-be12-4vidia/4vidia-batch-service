package init.data.DataParser.service.impl;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Book;
import init.data.DataParser.repository.BookRepository;
import init.data.DataParser.service.BookService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

  private final BookRepository bookRepository;

  @Override
  public Book create(ParsingDto dto) {
    if (exist(dto.getLongIsbn())) {
      throw new IllegalArgumentException("이미 등록된 도서입니다.");
    }

    Book book = new Book();
    book.setLongIsbn(dto.getLongIsbn());
    book.setTitle(dto.getTitle());
    book.setPriceStandard(dto.getPriceStandard());
    book.setImageUrl(dto.getImageUrl());
    book.setDescription(dto.getDescription());
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDate publishDate = LocalDate.parse(dto.getPublishedDate(), formatter);
    book.setPublishedDate(publishDate);

    return bookRepository.save(book);
  }

  @Override
  public boolean exist(Long isbn) {
    return bookRepository.existsByLongIsbn(isbn);
  }

  @Override
  public Book getBook(Long isbn) {
    if (bookRepository.existsByLongIsbn(isbn)){
      return bookRepository.findByLongIsbn(isbn);
    }
    return null;
  }
}
