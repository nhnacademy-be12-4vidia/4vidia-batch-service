package com.nhnacademy.book_data_batch.repository.bulk;

import com.nhnacademy.book_data_batch.batch.book.dto.BookImageDto;

import java.util.List;

public interface BulkBookImageRepository {

    void bulkInsert(List<BookImageDto> bookImages);
}
