package init.data.DataParser.service;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Book;

public interface BookService {

  Book create(ParsingDto dto);

  boolean exist(Long isbn);

  Book getBook(Long isbn);

}
