package com.nhnacademy.book_data_batch.service;

import com.nhnacademy.book_data_batch.entity.Author;
import java.util.List;

public interface AuthorService {

    Author save(String authorName);

    boolean isExistsByName(String authorName);

    Author getByName(String authorName);

    Author createByApi(Author author);

    List<Author> saveAll(Iterable<Author> authors);

}
