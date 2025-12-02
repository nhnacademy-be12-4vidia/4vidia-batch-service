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
    public Long findMinIdByEnrichmentStatus(BatchStatus status) {
        return queryFactory
                .select(batch.id.min())
                .from(batch)
                .where(batch.enrichmentStatus.eq(status))
                .fetchOne();
    }

    @Override
    public Long findMaxIdByEnrichmentStatus(BatchStatus status) {
        return queryFactory
                .select(batch.id.max())
                .from(batch)
                .where(batch.enrichmentStatus.eq(status))
                .fetchOne();
    }

    @Override
    public Page<BookEnrichmentTarget> findPendingForEnrichment(
            BatchStatus status,
            Long startId,
            Long endId,
            Pageable pageable) {

        // 데이터 조회
        List<BookEnrichmentTarget> content = queryFactory
                .select(Projections.constructor(
                        BookEnrichmentTarget.class,
                        book.id,
                        book.isbn,
                        batch.id
                ))
                .from(book)
                .join(batch).on(book.id.eq(batch.book.id))
                .where(
                        batch.enrichmentStatus.eq(status),
                        batch.id.goe(startId),
                        batch.id.lt(endId)
                )
                .orderBy(batch.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Count 쿼리 (지연 실행)
        JPAQuery<Long> countQuery = queryFactory
                .select(batch.count())
                .from(batch)
                .where(
                        batch.enrichmentStatus.eq(status),
                        batch.id.goe(startId),
                        batch.id.lt(endId)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
