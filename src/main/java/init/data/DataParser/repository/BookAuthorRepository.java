package init.data.DataParser.repository;

import init.data.DataParser.entity.Author;
import init.data.DataParser.entity.Book;
import init.data.DataParser.entity.BookAuthor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookAuthorRepository extends JpaRepository<BookAuthor, Long> {

  Boolean existsByAuthorNameAndBookLongIsbn(String authorName, Long isbn);

}
