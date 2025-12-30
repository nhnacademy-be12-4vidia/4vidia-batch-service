package com.nhnacademy.book_data_batch.domain.repository.custom;

import com.nhnacademy.book_data_batch.jobs.book_import.dto.BookImageDto;

import java.util.List;

public interface BookImageRepositoryCustom {

    void bulkInsert(List<BookImageDto> bookImages);
}
