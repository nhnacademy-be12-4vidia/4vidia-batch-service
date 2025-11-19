package init.data.DataParser.service;

import init.data.DataParser.entity.Author;

public interface AuthorService {

    Author save(String authorName, Long isbn);

    Boolean isExists(String name, Long isbn);

}
