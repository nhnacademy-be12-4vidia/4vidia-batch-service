package com.nhnacademy.book_data_batch.jobs.discount_reprice.writer;

import com.nhnacademy.book_data_batch.jobs.discount_reprice.dto.DiscountRepriceTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 변경된 가격 정보를 Elasticsearch에 부분 업데이트(Partial Update)하는 Writer.
 * 전체 문서를 갱신하지 않고 'priceSales' 필드만 변경
 */
@Slf4j
@RequiredArgsConstructor
public class EsPriceUpdateWriter implements ItemWriter<DiscountRepriceTarget> {

    private final ElasticsearchOperations elasticsearchOperations;
    private final String indexName;

    @Override
    public void write(Chunk<? extends DiscountRepriceTarget> chunk) throws Exception {
        if (chunk.isEmpty()) {
            return;
        }

        IndexCoordinates index = IndexCoordinates.of(indexName);

        // 1. Chunk 내 도서 ID 목록 추출
        List<String> chunkIds = chunk.getItems().stream()
                .map(item -> String.valueOf(item.bookId()))
                .toList();

        // 2. ES에 실제로 존재하는 ID 조회 (CriteriaQuery 사용)
        Criteria criteria = new Criteria("_id").in(chunkIds);
        CriteriaQuery query = new CriteriaQuery(criteria);
        
        // 성능 최적화: ID만 가져오도록 소스 필터 설정
        query.addSourceFilter(new FetchSourceFilter(true, new String[]{"_id"}, null));

        SearchHits<Object> searchHits = elasticsearchOperations.search(query, Object.class, index);
        
        Set<String> existingIds = searchHits.stream()
                .map(SearchHit::getId)
                .collect(Collectors.toSet());

        if (existingIds.isEmpty()) {
            log.debug("[EsPriceUpdateWriter] 업데이트 대상 도서가 ES에 하나도 존재하지 않음 (Chunk Size: {}).", chunk.size());
            return;
        }

        // 3. 존재하는 도서만 필터링하여 업데이트 쿼리 생성
        List<UpdateQuery> updates = new ArrayList<>();
        
        for (DiscountRepriceTarget item : chunk) {
            String docId = String.valueOf(item.bookId());

            // ES에 있는 경우만 업데이트 쿼리 생성
            if (existingIds.contains(docId)) {
                Map<String, Object> docMap = Map.of("priceSales", item.priceSales());

                UpdateQuery updateQuery = UpdateQuery.builder(docId)
                        .withDocument(Document.from(docMap))
                        .build();

                updates.add(updateQuery);
            }
        }

        // 4. Bulk Update 실행
        if (!updates.isEmpty()) {
            try {
                elasticsearchOperations.bulkUpdate(updates, index);
                log.debug("[EsPriceUpdateWriter] ES 가격 업데이트 완료: {}건 (요청: {}건 중)", updates.size(), chunk.size());
            } catch (Exception e) {
                // 진짜 에러(네트워크 등) 발생 시 재시도를 위해 던짐
                List<Long> failedIds = chunk.getItems().stream()
                        .map(DiscountRepriceTarget::bookId)
                        .toList();
                log.error("[EsPriceUpdateWriter] ES 가격 업데이트 실패. Failed IDs: {}", failedIds, e);
                throw e;
            }
        }
    }
}
