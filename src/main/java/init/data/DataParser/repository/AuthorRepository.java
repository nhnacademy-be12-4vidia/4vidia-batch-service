package init.data.DataParser.repository;

import init.data.DataParser.entity.Author;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    boolean existsByName(String name);

    Author findByName(String name);

    List<Author> findAllByNameIn(Collection<String> names);
}
