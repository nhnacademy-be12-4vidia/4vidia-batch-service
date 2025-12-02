package com.nhnacademy.book_data_batch.batch.enrichment.aladin.processor;

import com.nhnacademy.book_data_batch.batch.enrichment.aladin.client.AladinApiClient;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.AladinItemDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.BookEnrichmentTarget;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.EnrichmentResultDto;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.dto.EnrichmentResultDto.AuthorWithRole;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.exception.DailyQuotaExceededException;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.extractor.AladinAuthorExtractor;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.extractor.CategoryTagExtractor;
import com.nhnacademy.book_data_batch.batch.enrichment.aladin.mapper.AladinBookMapper;
import com.nhnacademy.book_data_batch.entity.Book;
import com.nhnacademy.book_data_batch.repository.BookRepository;
import com.nhnacademy.book_data_batch.service.openapi.AladinQuotaService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Aladin API로 도서 정보를 보강하는 Processor
 * 
 * <p>책임:
 * <ul>
 *   <li>BookRepository로 실제 Book 엔티티 조회 (JPA)</li>
 *   <li>Aladin API 호출 (AladinApiClient 위임)</li>
 *   <li>응답 데이터 매핑 (AladinBookMapper 위임)</li>
 *   <li>저자 추출 (AladinAuthorExtractor 위임)</li>
 *   <li>태그 추출 (CategoryTagExtractor 위임)</li>
 *   <li>일일 쿼터 제한 (Redis로 관리, API 키당 5,000건)</li>
 * </ul>
 * </p>
 * 
 * <p>쿼터 관리:
 * - Redis에 일일 사용량 저장 (AladinQuotaService)
 * - Job 재시작 시에도 사용량 유지
 * - 자정에 자동 리셋 (TTL)
 * </p>
 * 
 * <p>주의: Book 엔티티는 조회 후 detach하여 영속성 컨텍스트에서 분리.
 * Writer에서 JDBC로 bulk update하므로 JPA dirty checking 충돌 방지.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class AladinEnrichmentProcessor implements ItemProcessor<BookEnrichmentTarget, EnrichmentResultDto> {

    private final BookRepository bookRepository;
    private final EntityManager entityManager;
    private final AladinApiClient aladinApiClient;
    private final AladinBookMapper bookMapper;
    private final AladinAuthorExtractor authorExtractor;
    private final CategoryTagExtractor tagExtractor;
    private final AladinQuotaService quotaService;
    private final String apiKey;
    private final int partitionIndex;

    @Override
    public EnrichmentResultDto process(BookEnrichmentTarget target) {
        // Redis에서 일일 쿼터 체크 (API 호출 전에 확인)
        checkDailyQuota();
        
        Long bookId = target.bookId();
        String isbn13 = target.isbn13();

        if (!StringUtils.hasText(isbn13)) {
            return EnrichmentResultDto.failure(bookId, "ISBN13이 없음");
        }

        try {
            // JPA로 Book 엔티티 조회
            Optional<Book> bookOpt = bookRepository.findById(bookId);
            if (bookOpt.isEmpty()) {
                return EnrichmentResultDto.failure(bookId, "Book not found: " + bookId);
            }

            Book book = bookOpt.get();
            
            // 영속성 컨텍스트에서 분리 (Writer에서 JDBC로 update하므로 충돌 방지)
            entityManager.detach(book);
            
            return enrichBook(book, isbn13);

        } catch (DailyQuotaExceededException e) {
            // 쿼터 초과 예외는 상위로 전파 (Step 중단)
            throw e;
        } catch (Exception e) {
            log.error("Aladin 보강 실패: bookId={}, ISBN={}, error={}", bookId, isbn13, e.getMessage());
            return EnrichmentResultDto.failure(bookId, e.getMessage());
        }
    }

    /**
     * Book 보강 처리 (성공/실패 분기)
     */
    private EnrichmentResultDto enrichBook(Book book, String isbn13) {
        // Redis에 API 호출 카운트 증가 및 현재 사용량 조회
        long currentCount = quotaService.incrementAndGet(apiKey);
        
        if (currentCount % 500 == 0) {
            log.info("파티션 {} API 호출 진행: {}/{} (apiKey: {}***)", 
                    partitionIndex, currentCount, quotaService.getDailyQuotaLimit(), 
                    apiKey.substring(0, Math.min(8, apiKey.length())));
        }
        
        Optional<AladinItemDto> response = aladinApiClient.lookupByIsbn(isbn13, apiKey);

        if (response.isEmpty()) {
            return EnrichmentResultDto.failure(book.getId(), "Aladin API 응답 없음");
        }

        AladinItemDto aladinItem = response.get();

        // 각 컴포넌트에 책임 위임
        bookMapper.mapToBook(book, aladinItem);
        List<AuthorWithRole> authors = authorExtractor.extract(aladinItem);
        List<String> tags = tagExtractor.extract(aladinItem.categoryName());
        String coverUrl = aladinItem.cover();  // 표지 이미지 URL

        return EnrichmentResultDto.success(book, authors, tags, coverUrl);
    }

    /**
     * Redis에서 일일 쿼터 초과 여부 확인
     * - 5,000건 초과 시 DailyQuotaExceededException 발생
     * - 해당 파티션의 Step만 중단됨 (다른 파티션은 계속 진행)
     */
    private void checkDailyQuota() {
        if (quotaService.isQuotaExceeded(apiKey)) {
            long currentUsage = quotaService.getCurrentUsage(apiKey);
            log.warn("파티션 {} 일일 쿼터 소진 (Redis): {}/{} (apiKey: {}***)", 
                    partitionIndex, currentUsage, quotaService.getDailyQuotaLimit(),
                    apiKey.substring(0, Math.min(8, apiKey.length())));
            throw new DailyQuotaExceededException(partitionIndex, quotaService.getDailyQuotaLimit(), (int) currentUsage);
        }
    }

    /**
     * 현재 API 호출 횟수 반환 (테스트/모니터링용)
     */
    public long getApiCallCount() {
        return quotaService.getCurrentUsage(apiKey);
    }
    
    /**
     * 남은 쿼터 반환
     */
    public long getRemainingQuota() {
        return quotaService.getRemainingQuota(apiKey);
    }
}
