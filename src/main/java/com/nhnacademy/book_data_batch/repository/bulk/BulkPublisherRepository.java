package com.nhnacademy.book_data_batch.repository.bulk;

import java.util.Set;

public interface BulkPublisherRepository {

    void bulkInsert(Set<String> publisherNames);
}
