package com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto;

import com.nhnacademy.book_data_batch.entity.Book;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookSavedEvent {

    private List<Book> savedBooks;

}
