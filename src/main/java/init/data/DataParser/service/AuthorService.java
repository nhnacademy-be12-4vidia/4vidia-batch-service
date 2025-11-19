package init.data.DataParser.service;

import init.data.DataParser.entity.Author;
import java.util.List;

public interface AuthorService {

    Author save(String authorName);

    boolean isExistsByName(String authorName);

    Author getByName(String authorName);

    Author createByApi(Author author);

    List<Author> saveAll(Iterable<Author> authors);

}
