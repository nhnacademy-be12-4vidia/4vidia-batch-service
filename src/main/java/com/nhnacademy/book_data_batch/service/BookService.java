package com.nhnacademy.book_data_batch.service;

import com.nhnacademy.book_data_batch.dto.ParsingDto;
import com.nhnacademy.book_data_batch.entity.Book;
import java.util.List;

public interface BookService {

    Book create(ParsingDto dto);

    boolean exist(String isbn);

    Book getBook(String isbn);

    Book createByApi(Book book);

    List<Book> createAll(List<Book> newBooks);


}
