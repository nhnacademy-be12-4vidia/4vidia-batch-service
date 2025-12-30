package com.nhnacademy.book_data_batch.domain.repository.custom;

import java.util.List;

public interface BookTagRepositoryCustom {

    void bulkInsert(List<long[]> bookTagPairs);
}
