package com.nhnacademy.book_data_batch.service.impl;

import com.nhnacademy.book_data_batch.repository.PublisherRepository;
import com.nhnacademy.book_data_batch.service.PublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PublisherServiceImpl implements PublisherService {

    private final PublisherRepository publisherRepository;
}
