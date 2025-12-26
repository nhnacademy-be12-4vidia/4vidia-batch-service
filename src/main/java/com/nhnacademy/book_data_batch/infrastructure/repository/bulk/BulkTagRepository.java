package com.nhnacademy.book_data_batch.infrastructure.repository.bulk;

import java.util.Set;

public interface BulkTagRepository {

    void bulkInsert(Set<String> tagNames);
}
