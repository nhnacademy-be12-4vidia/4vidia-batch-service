package init.data.DataParser.repository;

import init.data.DataParser.entity.Book;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {


  Book findByLongIsbn(Long longIsbn);

  boolean existsByLongIsbn(Long longIsbn);
}
