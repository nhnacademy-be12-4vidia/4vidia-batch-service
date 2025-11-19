package init.data.DataParser.service.impl;

import init.data.DataParser.entity.Author;
import init.data.DataParser.repository.AuthorRepository;
import init.data.DataParser.repository.BookAuthorRepository;
import init.data.DataParser.service.AuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;
    private final BookAuthorRepository bookAuthorRepository;

    @Override
    public Author save(String authorName, Long isbn) {
        if (isExists(authorName, isbn)) {
            throw new IllegalArgumentException();
        }
        Author author = new Author();
        author.setName(authorName);
        return authorRepository.save(author);
    }


    @Override
    public Boolean isExists(String name, Long isbn) {
        return bookAuthorRepository.existsByAuthorNameAndBookLongIsbn(name, isbn);
    }
}
