package com.nhnacademy.book_data_batch.repository.querydsl;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.BookEnrichmentTarget;
import com.nhnacademy.book_data_batch.entity.QBatch;
import com.nhnacademy.book_data_batch.entity.QBook;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

@RequiredArgsConstructor
public class QuerydslBatchRepositoryImpl implements QuerydslBatchRepository {

    private final JPAQueryFactory queryFactory;

    private static final QBatch batch = QBatch.batch;
    private static final QBook book = QBook.book;

    @Override
    public List<BookEnrichmentTarget> findAllPending() {
        return queryFactory
                .select(Projections.constructor(
                        BookEnrichmentTarget.class,
                        book.id,
                        book.isbn,
                        batch.id
                ))
                .from(batch)
                .join(batch.book, book)
                .where(batch.enrichmentStatus.eq(BatchStatus.PENDING))
                .orderBy(batch.id.asc())
                .fetch();
    }
}
