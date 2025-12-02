package com.nhnacademy.book_data_batch.service;

import com.nhnacademy.book_data_batch.dto.ParsingDto;
import com.nhnacademy.book_data_batch.entity.Author;
import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.entity.BookAuthor;
import java.util.List;

public interface BookAuthorService {

    BookAuthor createByApi(BookAuthor bookAuthor);

    List<BookAuthor> createAll(List<BookAuthor> bookAuthors);

}
