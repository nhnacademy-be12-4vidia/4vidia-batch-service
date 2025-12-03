package com.nhnacademy.book_data_batch.repository.querydsl;

import com.nhnacademy.book_data_batch.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.dto.BookEmbeddingBasic;
import com.nhnacademy.book_data_batch.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.entity.*;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;

@RequiredArgsConstructor
public class QuerydslBatchRepositoryImpl implements QuerydslBatchRepository {

    private final JPAQueryFactory queryFactory;

    private static final QBatch batch = QBatch.batch;
    private static final QBook book = QBook.book;
    private static final QPublisher publisher = QPublisher.publisher;
    private static final QBookAuthor bookAuthor = QBookAuthor.bookAuthor;
    private static final QAuthor author = QAuthor.author;
    private static final QBookTag bookTag = QBookTag.bookTag;
    private static final QTag tag = QTag.tag;

    @Override
    public List<BookBatchTarget> findPendingEnrichmentStatusBook() {
        return queryFactory
                .select(Projections.constructor(
                        BookBatchTarget.class,
                        book.id,
                        book.isbn,
                        batch.id
                ))
                .from(batch)
                .join(batch.book, book)
                .where(batch.enrichmentStatus.eq(BatchStatus.PENDING)
                        .and(batch.enrichmentRetryCount.lt(3)))
                .orderBy(batch.id.asc())
                .fetch();
    }

    @Override
    public List<BookEmbeddingTarget> findPendingEmbeddingStatusBook() {
        // 1. 기본 정보 조회 (Batch + Book + Publisher)
        List<BookEmbeddingBasic> basics = queryFactory
                .select(Projections.constructor(
                        BookEmbeddingBasic.class,
                        book.id,
                        batch.id,
                        book.isbn,
                        book.title,
                        book.description,
                        publisher.name,
                        book.priceSales,
                        book.stock
                ))
                .from(batch)
                .join(batch.book, book)
                .leftJoin(book.publisher, publisher)
                .where(batch.enrichmentStatus.eq(BatchStatus.COMPLETED)
                        .and(batch.embeddingStatus.eq(BatchStatus.PENDING))
                        .and(batch.embeddingRetryCount.lt(3)))
                .orderBy(batch.id.asc())
                .fetch();

        if (basics.isEmpty()) {
            return List.of();
        }

        // bookId 목록 추출
        List<Long> bookIds = basics.stream()
                .map(BookEmbeddingBasic::bookId)
                .toList();

        // 2. 저자 정보 조회 (bookId -> 저자명 리스트)
        Map<Long, List<String>> authorMap = queryFactory
                .from(bookAuthor)
                .join(bookAuthor.author, author)
                .where(bookAuthor.book.id.in(bookIds))
                .transform(groupBy(bookAuthor.book.id).as(list(author.name)));

        // 3. 태그 정보 조회 (bookId -> 태그명 리스트)
        Map<Long, List<String>> tagMap = queryFactory
                .from(bookTag)
                .join(bookTag.tag, tag)
                .where(bookTag.book.id.in(bookIds))
                .transform(groupBy(bookTag.book.id).as(list(tag.name)));

        // 4. 조합하여 BookEmbeddingTarget 생성
        return basics.stream()
                .map(basic -> {
                    String authors = joinToString(authorMap.get(basic.bookId()));
                    String tags = joinToString(tagMap.get(basic.bookId()));
                    return basic.toTarget(authors, tags);
                })
                .toList();
    }

    /**
     * 리스트를 쉼표로 구분된 문자열로 변환
     */
    private String joinToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(",", list);
    }
}
