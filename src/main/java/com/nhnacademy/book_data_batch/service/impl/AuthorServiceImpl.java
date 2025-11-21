package com.nhnacademy.book_data_batch.service.impl;

import com.nhnacademy.book_data_batch.entity.Author;
import com.nhnacademy.book_data_batch.repository.AuthorRepository;
import com.nhnacademy.book_data_batch.service.AuthorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthorServiceImpl implements AuthorService {
    private final AuthorRepository authorRepository;

    @Override
    public boolean isExistsByName(String authorName) {
        return authorRepository.existsByName(authorName);
    }

    @Override
    public Author getByName(String authorName) {
        if (!authorRepository.existsByName(authorName)) {
            throw new IllegalArgumentException("등록되지 않은 저자입니다.");
        }
        return authorRepository.findByName(authorName);
    }

    @Override
    public List<Author> saveAll(Iterable<Author> authors) {
        authorRepository.saveAll(authors);
        return null;
    }

    @Override
    public Author save(String authorName) {
        if (authorRepository.existsByName(authorName)) {
            throw new IllegalArgumentException("이미 등록된 저자입니다.");
        }
        Author author = new Author();
        author.setName(authorName);
        return authorRepository.save(author);
    }

    @Override
    public Author createByApi(Author author) {
        if (authorRepository.existsByName(author.getName())) {
            throw new IllegalArgumentException("이미 등록된 저자입니다.");
        }
        return authorRepository.save(author);
    }
}
