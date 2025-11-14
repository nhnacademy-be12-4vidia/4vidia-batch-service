package init.data.DataParser.repository;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {


}
