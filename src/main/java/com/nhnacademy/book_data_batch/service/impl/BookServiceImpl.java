package com.nhnacademy.book_data_batch.service.impl;

import com.nhnacademy.book_data_batch.dto.ParsingDto;
import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.repository.BookRepository;
import com.nhnacademy.book_data_batch.service.BookService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Override
    public Book create(ParsingDto dto) {
        if (exist(dto.getIsbn())) {
            throw new IllegalArgumentException("이미 등록된 도서입니다.");
        }

        Book book = new Book();
        book.setIsbn(dto.getIsbn());
        book.setTitle(dto.getTitle());
        book.setPriceStandard(dto.getPriceStandard());
        book.setDescription(dto.getDescription());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate publishDate = LocalDate.parse(dto.getPublishedDate(), formatter);
        book.setPublishedDate(publishDate);

        return bookRepository.save(book);
    }

    @Override
    public boolean exist(String isbn) {
        return bookRepository.existsByIsbn(isbn);
    }

    @Override
    public Book getBook(String isbn) {
        if (bookRepository.existsByIsbn(isbn)) {
            return bookRepository.findByIsbn(isbn);
        }
        return null;
    }

    @Override
    public Book createByApi(Book book) {
        if (exist(book.getIsbn())) {
            throw new IllegalArgumentException("이미 등록된 도서입니다.");
        }
        return bookRepository.save(book);
    }

    @Override
    public List<Book> createAll(List<Book> newBooks) {

        List<String> isbns = newBooks.stream()
            .map(Book::getIsbn)
            .toList();

        List<Book> existingBooks = bookRepository.findAllByIsbnIn(isbns);

        Set<String> existingIsbnSet = existingBooks.stream().map(Book::getIsbn)
            .collect(Collectors.toSet());

        List<Book> booksToSave = newBooks.stream()
            .filter(b -> !existingIsbnSet.contains(b.getIsbn())).toList();

        if (!booksToSave.isEmpty()) {
            return bookRepository.saveAll(booksToSave);
        }
        return List.of();
    }
}
