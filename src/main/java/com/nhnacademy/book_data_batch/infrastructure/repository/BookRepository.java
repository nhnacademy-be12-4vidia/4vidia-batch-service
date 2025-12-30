package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.Book;
import com.nhnacademy.book_data_batch.infrastructure.repository.bulk.BulkBookRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

import java.time.LocalDateTime;

public interface BookRepository extends JpaRepository<Book, Long>, BulkBookRepository {

    List<Book> findAllByIsbnIn(Collection<String> isbns);

    boolean existsByDescriptionContaining(String imageUrl);

    // 최적화를 위해 최근 수정된 도서만 조회 (Description만 필요하지만 편의상 엔티티 조회 후 Getter 사용)
    // 성능을 위해 @Query로 description만 가져오는 게 좋음
    @Query("SELECT b.description " +
            "FROM Book b " +
            "WHERE b.updatedAt > :updatedAt " +
            "AND b.description IS NOT NULL")
    List<String> findDescriptionsByUpdatedAtAfter(@Param("updatedAt") LocalDateTime updatedAt);

}
