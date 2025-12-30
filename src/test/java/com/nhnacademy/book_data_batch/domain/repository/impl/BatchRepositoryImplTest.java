package com.nhnacademy.book_data_batch.domain.repository.impl;

import com.nhnacademy.book_data_batch.domain.repository.impl.BatchRepositoryImpl;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.EnrichmentFailureDto;
import com.nhnacademy.book_data_batch.jobs.embedding.dto.EmbeddingFailureDto;
import com.nhnacademy.book_data_batch.domain.entity.Batch;
import com.nhnacademy.book_data_batch.domain.entity.Book;
import com.nhnacademy.book_data_batch.domain.enums.BatchStatus;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import com.nhnacademy.book_data_batch.domain.repository.BatchRepository;
import com.nhnacademy.book_data_batch.domain.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({JdbcExecutor.class, BatchRepositoryImpl.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("BatchRepositoryImpl 통합 테스트")
class BatchRepositoryImplTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BatchRepository batchRepository;

    private Book createBook(String isbn, String title, int volumeNumber) {
        Book book = Book.builder()
                .isbn(isbn)
                .title(title)
                .volumeNumber(volumeNumber)
                .build();
        return bookRepository.save(book);
    }

    @BeforeEach
    void setUp() {
        batchRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    @DisplayName("bulkInsert: 빈 리스트일 때 아무것도 실행되지 않음")
    void bulkInsert_emptyList_noExecution() {
        List<Batch> batches = List.of();

        batchRepository.bulkInsert(batches);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM batch", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkInsert: 여러 Batch 레코드 삽입")
    void bulkInsert_multipleBatches_insertsCorrectly() {
        Book book1 = createBook("1234567890123", "Test Book 1", 1);
        Book book2 = createBook("1234567890124", "Test Book 2", 2);
        Book book3 = createBook("1234567890125", "Test Book 3", 3);

        Batch batch1 = new Batch(book1);
        Batch batch2 = new Batch(book2);
        Batch batch3 = new Batch(book3);

        batchRepository.bulkInsert(List.of(batch1, batch2, batch3));

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM batch", Integer.class);
        assertThat(count).isEqualTo(3);

        List<Integer> statuses = jdbcTemplate.queryForList("SELECT enrichment_status FROM batch", Integer.class);
        assertThat(statuses).allMatch(s -> s == BatchStatus.PENDING.getCode());
    }

    @Test
    @DisplayName("bulkInsert: PENDING 상태로 초기화")
    void bulkInsert_initializesWithPendingStatus() {
        Book book = createBook("1234567890123", "Test Book", 1);

        Batch batch = new Batch(book);

        batchRepository.bulkInsert(List.of(batch));

        Integer enrichmentStatus = jdbcTemplate.queryForObject("SELECT enrichment_status FROM batch WHERE book_id = ?", Integer.class, book.getId());
        Integer embeddingStatus = jdbcTemplate.queryForObject("SELECT embedding_status FROM batch WHERE book_id = ?", Integer.class, book.getId());

        assertThat(enrichmentStatus).isEqualTo(BatchStatus.PENDING.getCode());
        assertThat(embeddingStatus).isEqualTo(BatchStatus.PENDING.getCode());
    }

    @Test
    @DisplayName("bulkUpdateEnrichmentStatus: 빈 리스트일 때 아무것도 실행되지 않음")
    void bulkUpdateEnrichmentStatus_emptyList_noExecution() {
        List<Long> batchIds = List.of();

        batchRepository.bulkUpdateEnrichmentStatus(batchIds, BatchStatus.COMPLETED);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM batch", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkUpdateEnrichmentStatus: 상태 업데이트")
    void bulkUpdateEnrichmentStatus_updatesStatus() {
        Book book1 = createBook("1234567890123", "Test Book 1", 1);
        Book book2 = createBook("1234567890124", "Test Book 2", 2);

        Batch batch1 = new Batch(book1);
        Batch batch2 = new Batch(book2);
        batch1 = batchRepository.save(batch1);
        batch2 = batchRepository.save(batch2);

        List<Long> batchIds = List.of(batch1.getId(), batch2.getId());

        batchRepository.bulkUpdateEnrichmentStatus(batchIds, BatchStatus.COMPLETED);

        List<Integer> statuses = jdbcTemplate.queryForList("SELECT enrichment_status FROM batch WHERE batch_id IN (?, ?)", Integer.class, batch1.getId(), batch2.getId());
        assertThat(statuses).allMatch(s -> s == BatchStatus.COMPLETED.getCode());
    }

    @Test
    @DisplayName("bulkUpdateEnrichmentFailed: 빈 리스트일 때 아무것도 실행되지 않음")
    void bulkUpdateEnrichmentFailed_emptyList_noExecution() {
        List<EnrichmentFailureDto> failedBatches = List.of();

        batchRepository.bulkUpdateEnrichmentFailed(failedBatches);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM batch", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkUpdateEnrichmentFailed: 에러 메시지 저장 및 상태 PENDING 유지")
    void bulkUpdateEnrichmentFailed_savesErrorMessageAndKeepsPendingStatus() {
        Book book = createBook("1234567890123", "Test Book", 1);

        Batch batch = new Batch(book);
        batch = batchRepository.save(batch);

        jdbcTemplate.update("UPDATE batch SET enrichment_status = ? WHERE batch_id = ?", BatchStatus.FAILED.getCode(), batch.getId());

        List<EnrichmentFailureDto> failedBatches = List.of(
                new EnrichmentFailureDto(batch.getId(), "Enrichment failed: timeout error")
        );

        batchRepository.bulkUpdateEnrichmentFailed(failedBatches);

        Integer enrichmentStatus = jdbcTemplate.queryForObject("SELECT enrichment_status FROM batch WHERE batch_id = ?", Integer.class, batch.getId());
        String errorMessage = jdbcTemplate.queryForObject("SELECT error_message FROM batch WHERE batch_id = ?", String.class, batch.getId());

        assertThat(enrichmentStatus).isEqualTo(BatchStatus.PENDING.getCode());
        assertThat(errorMessage).isEqualTo("Enrichment failed: timeout error");
    }

    @Test
    @DisplayName("bulkUpdateEnrichmentFailed: 에러 메시지 500자 초과 시 잘림")
    void bulkUpdateEnrichmentFailed_truncatesLongErrorMessage() {
        Book book = createBook("1234567890123", "Test Book", 1);

        Batch batch = new Batch(book);
        batch = batchRepository.save(batch);

        jdbcTemplate.update("UPDATE batch SET enrichment_status = ? WHERE batch_id = ?", BatchStatus.FAILED.getCode(), batch.getId());

        String longMessage = "A".repeat(600);
        List<EnrichmentFailureDto> failedBatches = List.of(
                new EnrichmentFailureDto(batch.getId(), longMessage)
        );

        batchRepository.bulkUpdateEnrichmentFailed(failedBatches);

        String errorMessage = jdbcTemplate.queryForObject("SELECT error_message FROM batch WHERE batch_id = ?", String.class, batch.getId());

        assertThat(errorMessage).hasSize(500);
        assertThat(errorMessage).doesNotContain("A".repeat(501));
    }

    @Test
    @DisplayName("bulkUpdateEnrichmentFailed: null 에러 메시지 처리")
    void bulkUpdateEnrichmentFailed_handlesNullErrorMessage() {
        Book book = createBook("1234567890123", "Test Book", 1);

        Batch batch = new Batch(book);
        batch = batchRepository.save(batch);

        jdbcTemplate.update("UPDATE batch SET enrichment_status = ? WHERE batch_id = ?", BatchStatus.FAILED.getCode(), batch.getId());

        List<EnrichmentFailureDto> failedBatches = List.of(
                new EnrichmentFailureDto(batch.getId(), null)
        );

        batchRepository.bulkUpdateEnrichmentFailed(failedBatches);

        String errorMessage = jdbcTemplate.queryForObject("SELECT error_message FROM batch WHERE batch_id = ?", String.class, batch.getId());

        assertThat(errorMessage).isNull();
    }

    @Test
    @DisplayName("bulkUpdateEmbeddingStatus: 빈 리스트일 때 아무것도 실행되지 않음")
    void bulkUpdateEmbeddingStatus_emptyList_noExecution() {
        List<Long> batchIds = List.of();

        batchRepository.bulkUpdateEmbeddingStatus(batchIds, BatchStatus.COMPLETED);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM batch", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkUpdateEmbeddingStatus: 상태 업데이트")
    void bulkUpdateEmbeddingStatus_updatesStatus() {
        Book book1 = createBook("1234567890123", "Test Book 1", 1);
        Book book2 = createBook("1234567890124", "Test Book 2", 2);

        Batch batch1 = new Batch(book1);
        Batch batch2 = new Batch(book2);
        batch1 = batchRepository.save(batch1);
        batch2 = batchRepository.save(batch2);

        jdbcTemplate.update("UPDATE batch SET enrichment_status = ?, embedding_status = ? WHERE batch_id IN (?, ?)",
            BatchStatus.COMPLETED.getCode(), BatchStatus.FAILED.getCode(), batch1.getId(), batch2.getId());

        List<Long> batchIds = List.of(batch1.getId(), batch2.getId());

        batchRepository.bulkUpdateEmbeddingStatus(batchIds, BatchStatus.COMPLETED);

        List<Integer> statuses = jdbcTemplate.queryForList("SELECT embedding_status FROM batch WHERE batch_id IN (?, ?)", Integer.class, batch1.getId(), batch2.getId());
        assertThat(statuses).allMatch(s -> s == BatchStatus.COMPLETED.getCode());
    }

    @Test
    @DisplayName("bulkUpdateEmbeddingFailed: 빈 리스트일 때 아무것도 실행되지 않음")
    void bulkUpdateEmbeddingFailed_emptyList_noExecution() {
        List<EmbeddingFailureDto> failedBatches = List.of();

        batchRepository.bulkUpdateEmbeddingFailed(failedBatches);

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM batch", Integer.class);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("bulkUpdateEmbeddingFailed: 에러 메시지 저장 및 상태 PENDING 유지")
    void bulkUpdateEmbeddingFailed_savesErrorMessageAndKeepsPendingStatus() {
        Book book = createBook("1234567890123", "Test Book", 1);

        Batch batch = new Batch(book);
        batch = batchRepository.save(batch);

        jdbcTemplate.update("UPDATE batch SET enrichment_status = ?, embedding_status = ? WHERE batch_id = ?",
            BatchStatus.COMPLETED.getCode(), BatchStatus.FAILED.getCode(), batch.getId());

        List<EmbeddingFailureDto> failedBatches = List.of(
                new EmbeddingFailureDto(batch.getId(), "Embedding failed: API error")
        );

        batchRepository.bulkUpdateEmbeddingFailed(failedBatches);

        Integer embeddingStatus = jdbcTemplate.queryForObject("SELECT embedding_status FROM batch WHERE batch_id = ?", Integer.class, batch.getId());
        String errorMessage = jdbcTemplate.queryForObject("SELECT error_message FROM batch WHERE batch_id = ?", String.class, batch.getId());

        assertThat(embeddingStatus).isEqualTo(BatchStatus.PENDING.getCode());
        assertThat(errorMessage).isEqualTo("Embedding failed: API error");
    }

    @Test
    @DisplayName("bulkUpdateEmbeddingFailed: 에러 메시지 500자 초과 시 잘림")
    void bulkUpdateEmbeddingFailed_truncatesLongErrorMessage() {
        Book book = createBook("1234567890123", "Test Book", 1);

        Batch batch = new Batch(book);
        batch = batchRepository.save(batch);

        jdbcTemplate.update("UPDATE batch SET enrichment_status = ?, embedding_status = ? WHERE batch_id = ?",
            BatchStatus.COMPLETED.getCode(), BatchStatus.FAILED.getCode(), batch.getId());

        String longMessage = "B".repeat(600);
        List<EmbeddingFailureDto> failedBatches = List.of(
                new EmbeddingFailureDto(batch.getId(), longMessage)
        );

        batchRepository.bulkUpdateEmbeddingFailed(failedBatches);

        String errorMessage = jdbcTemplate.queryForObject("SELECT error_message FROM batch WHERE batch_id = ?", String.class, batch.getId());

        assertThat(errorMessage).hasSize(500);
    }

    @Test
    @DisplayName("deleteAllCompleted: COMPLETED 상태의 모든 Batch 삭제")
    void deleteAllCompleted_deletesAllCompletedBatches() {
        Book book1 = createBook("1234567890123", "Test Book 1", 1);
        Book book2 = createBook("1234567890124", "Test Book 2", 2);
        Book book3 = createBook("1234567890125", "Test Book 3", 3);

        Batch batch1 = new Batch(book1);
        Batch batch2 = new Batch(book2);
        Batch batch3 = new Batch(book3);

        batch1 = batchRepository.save(batch1);
        batch2 = batchRepository.save(batch2);
        batch3 = batchRepository.save(batch3);

        jdbcTemplate.update("UPDATE batch SET enrichment_status = ?, embedding_status = ? WHERE batch_id = ?",
            BatchStatus.COMPLETED.getCode(), BatchStatus.COMPLETED.getCode(), batch1.getId());
        jdbcTemplate.update("UPDATE batch SET enrichment_status = ?, embedding_status = ? WHERE batch_id = ?",
            BatchStatus.COMPLETED.getCode(), BatchStatus.COMPLETED.getCode(), batch2.getId());
        jdbcTemplate.update("UPDATE batch SET enrichment_status = ?, embedding_status = ? WHERE batch_id = ?",
            BatchStatus.PENDING.getCode(), BatchStatus.PENDING.getCode(), batch3.getId());

        batchRepository.deleteAllCompleted();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM batch", Integer.class);
        assertThat(count).isEqualTo(1);

        Long remainingId = jdbcTemplate.queryForObject("SELECT batch_id FROM batch", Long.class);
        assertThat(remainingId).isEqualTo(batch3.getId());
    }

    @Test
    @DisplayName("deleteAllCompleted: COMPLETED 상태 레코드가 없을 때")
    void deleteAllCompleted_noCompletedRecords_noDeletion() {
        Book book1 = createBook("1234567890123", "Test Book 1", 1);
        Book book2 = createBook("1234567890124", "Test Book 2", 2);

        Batch batch1 = new Batch(book1);
        Batch batch2 = new Batch(book2);

        batch1 = batchRepository.save(batch1);
        batch2 = batchRepository.save(batch2);

        jdbcTemplate.update("UPDATE batch SET enrichment_status = ?, embedding_status = ? WHERE batch_id = ?",
            BatchStatus.PENDING.getCode(), BatchStatus.PENDING.getCode(), batch1.getId());
        jdbcTemplate.update("UPDATE batch SET enrichment_status = ?, embedding_status = ? WHERE batch_id = ?",
            BatchStatus.FAILED.getCode(), BatchStatus.PENDING.getCode(), batch2.getId());

        batchRepository.deleteAllCompleted();

        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM batch", Integer.class);
        assertThat(count).isEqualTo(2);
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
