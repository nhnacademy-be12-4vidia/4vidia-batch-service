package com.nhnacademy.book_data_batch.domain.repository.custom;

import java.util.Set;

public interface TagRepositoryCustom {

    void bulkInsert(Set<String> tagNames);
}
