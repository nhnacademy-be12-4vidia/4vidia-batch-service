package com.nhnacademy.book_data_batch.domain.repository.custom;

import java.util.Set;

public interface PublisherRepositoryCustom {

    void bulkInsert(Set<String> publisherNames);
}
