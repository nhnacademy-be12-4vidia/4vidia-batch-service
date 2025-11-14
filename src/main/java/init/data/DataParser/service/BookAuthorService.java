package init.data.DataParser.service;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Author;
import init.data.DataParser.entity.Book;
import init.data.DataParser.entity.BookAuthor;

public interface BookAuthorService {

  BookAuthor save(Book book, Author author);

  Boolean isExists(ParsingDto dto);

}
