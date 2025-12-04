package com.nhnacademy.book_data_batch.service.impl;

import com.nhnacademy.book_data_batch.repository.BookRepository;
import com.nhnacademy.book_data_batch.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
}
