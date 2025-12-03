package com.nhnacademy.book_data_batch.entity;

import com.nhnacademy.book_data_batch.entity.converters.BatchStatusConverter;
import com.nhnacademy.book_data_batch.entity.enums.BatchStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * batch의 상태를 나타내는 엔티티 입니다.
 * 배치 작업의 시작 시 생성되고, 작업이 완료되면 삭제됩니다.
 */

@Entity
@Table(name = "batch", indexes = {
    @Index(name = "idx_batch_book_id", columnList = "book_id"),
    @Index(name = "idx_batch_enrichment_status", columnList = "enrichment_status"),
    @Index(name = "idx_batch_embedding_status", columnList = "embedding_status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Batch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "batch_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Book book;

    @Column(name = "enrichment_status", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    @Convert(converter = BatchStatusConverter.class)
    private BatchStatus enrichmentStatus = BatchStatus.PENDING;

    @Column(name = "embedding_status", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    @Convert(converter = BatchStatusConverter.class)
    private BatchStatus embeddingStatus = BatchStatus.PENDING;

    @Column(name = "enrichment_retry_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer enrichmentRetryCount = 0;

    @Column(name = "embedding_retry_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer embeddingRetryCount = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Builder
    public Batch(Book book) {
        this.book = book;
    }
}
