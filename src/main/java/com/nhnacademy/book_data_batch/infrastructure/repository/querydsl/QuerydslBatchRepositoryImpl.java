package com.nhnacademy.book_data_batch.infrastructure.repository.querydsl;

import com.nhnacademy.book_data_batch.batch.dto.BookBatchTarget;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.BookEmbeddingBasic;
import com.nhnacademy.book_data_batch.batch.enrichment.embedding.dto.BookEmbeddingTarget;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.domain.QAuthor;
import com.nhnacademy.book_data_batch.domain.QBatch;
import com.nhnacademy.book_data_batch.domain.QBook;
import com.nhnacademy.book_data_batch.domain.QBookAuthor;
import com.nhnacademy.book_data_batch.domain.QBookTag;
import com.nhnacademy.book_data_batch.domain.QPublisher;
import com.nhnacademy.book_data_batch.domain.QTag;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
//                        .and(book.priceStandard.isNotNull())
//                        .and(book.priceStandard.gt(0)))
                .orderBy(book.publishedDate.desc())
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
//                .where(batch.enrichmentStatus.eq(BatchStatus.PENDING)) // 테스트용: 알라딘 보강 생략
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
        // Hibernate 6 호환을 위해 transform() 대신 일반 쿼리 사용
        List<Tuple> authorTuples = queryFactory
                .select(bookAuthor.book.id, author.name)
                .from(bookAuthor)
                .join(bookAuthor.author, author)
                .where(bookAuthor.book.id.in(bookIds))
                .fetch();

        Map<Long, List<String>> authorMap = new HashMap<>();
        for (Tuple tuple : authorTuples) {
            Long bookId = tuple.get(bookAuthor.book.id);
            String authorName = tuple.get(author.name);
            authorMap.computeIfAbsent(bookId, k -> new ArrayList<>()).add(authorName);
        }

        // 3. 태그 정보 조회 (bookId -> 태그명 리스트)
        List<Tuple> tagTuples = queryFactory
                .select(bookTag.book.id, tag.name)
                .from(bookTag)
                .join(bookTag.tag, tag)
                .where(bookTag.book.id.in(bookIds))
                .fetch();

        Map<Long, List<String>> tagMap = new HashMap<>();
        for (Tuple tuple : tagTuples) {
            Long bookId = tuple.get(bookTag.book.id);
            String tagName = tuple.get(tag.name);
            tagMap.computeIfAbsent(bookId, k -> new ArrayList<>()).add(tagName);
        }

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
