package com.nhnacademy.book_data_batch.service.impl;

import com.nhnacademy.book_data_batch.dto.ParsingDto;
import com.nhnacademy.book_data_batch.entity.Author;
import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.entity.BookAuthor;
import com.nhnacademy.book_data_batch.repository.BookAuthorRepository;
import com.nhnacademy.book_data_batch.service.BookAuthorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookAuthorServiceImpl implements BookAuthorService {

    private final BookAuthorRepository bookAuthorRepository;

    @Override
    public BookAuthor createByApi(BookAuthor bookAuthor) {
        return bookAuthorRepository.save(bookAuthor);
    }

    @Override
    public List<BookAuthor> createAll(List<BookAuthor> bookAuthors) {
        if (!bookAuthors.isEmpty()) {
            bookAuthorRepository.saveAll(bookAuthors);
        }
        return List.of();
    }
}
