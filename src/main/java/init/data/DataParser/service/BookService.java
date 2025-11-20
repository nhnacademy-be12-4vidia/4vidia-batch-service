package init.data.DataParser.service;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Book;
import java.util.List;

public interface BookService {

    Book create(ParsingDto dto);

    boolean exist(String isbn);

    Book getBook(String isbn);

    Book createByApi(Book book);

    List<Book> createAll(List<Book> booksToSave);


}
