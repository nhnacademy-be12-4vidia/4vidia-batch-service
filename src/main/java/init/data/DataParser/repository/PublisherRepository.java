package init.data.DataParser.repository;

import init.data.DataParser.entity.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {

  Boolean existsByName(String name);

}
