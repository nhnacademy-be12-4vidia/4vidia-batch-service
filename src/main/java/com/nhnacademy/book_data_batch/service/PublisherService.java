package com.nhnacademy.book_data_batch.service;

import com.nhnacademy.book_data_batch.dto.ParsingDto;
import com.nhnacademy.book_data_batch.entity.Publisher;
import java.util.List;

public interface PublisherService {

    Publisher save(ParsingDto dto);

    Boolean isExists(String name);

    Publisher getByName(String publisherName);

    Publisher createByApi(Publisher publisher);

    List<Publisher> saveAll(Iterable<Publisher> publishers);

}
