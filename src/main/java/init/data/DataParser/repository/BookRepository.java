package init.data.DataParser.repository;

import init.data.DataParser.entity.Book;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {


    Book findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    List<Book> findAllByIsbnIn(Collection<String> isbns);

}
