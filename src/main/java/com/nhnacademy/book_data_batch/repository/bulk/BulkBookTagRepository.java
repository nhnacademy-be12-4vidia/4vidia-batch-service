package com.nhnacademy.book_data_batch.repository.bulk;

import java.util.List;

public interface BulkBookTagRepository {

    void bulkInsert(List<long[]> bookTagPairs);
}
