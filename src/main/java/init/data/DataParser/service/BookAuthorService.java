package init.data.DataParser.service;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Author;
import init.data.DataParser.entity.Book;
import init.data.DataParser.entity.BookAuthor;
import java.util.List;

public interface BookAuthorService {

    BookAuthor save(Book book, Author author);

    Boolean isExists(ParsingDto dto);

    BookAuthor createByApi(BookAuthor bookAuthor);

    List<BookAuthor> createAll(List<BookAuthor> bookAuthors);

}
