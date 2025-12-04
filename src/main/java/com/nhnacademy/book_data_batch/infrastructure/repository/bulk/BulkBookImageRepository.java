package com.nhnacademy.book_data_batch.infrastructure.repository.bulk;

import com.nhnacademy.book_data_batch.batch.dto.BookImageDto;

import java.util.List;

public interface BulkBookImageRepository {

    void bulkInsert(List<BookImageDto> bookImages);
}
