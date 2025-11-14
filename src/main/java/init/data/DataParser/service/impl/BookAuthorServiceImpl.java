package init.data.DataParser.service.impl;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Author;
import init.data.DataParser.entity.Book;
import init.data.DataParser.entity.BookAuthor;
import init.data.DataParser.repository.BookAuthorRepository;
import init.data.DataParser.service.BookAuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookAuthorServiceImpl implements BookAuthorService {

  private final BookAuthorRepository bookAuthorRepository;

  @Override
  public BookAuthor save(Book book, Author author) {
    BookAuthor bookAuthor = new BookAuthor();
    bookAuthor.setBook(book);
    bookAuthor.setAuthor(author);
    return bookAuthorRepository.save(bookAuthor);
  }

  @Override
  public Boolean isExists(ParsingDto dto) {
    return null;
  }
}
