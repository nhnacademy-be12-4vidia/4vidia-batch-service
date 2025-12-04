package com.nhnacademy.book_data_batch.infrastructure.repository.bulk;

import java.util.Set;

public interface BulkAuthorRepository {

    void bulkInsert(Set<String> authorNames);
}
