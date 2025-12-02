package com.nhnacademy.book_data_batch.batch.book.cache;

import com.nhnacademy.book_data_batch.entity.Publisher;
import com.nhnacademy.book_data_batch.repository.PublisherRepository;

/**
 * 참조 데이터(Publisher) 캐시 인터페이스
 * - 작가는 알라딘 API에서 처리하므로 Publisher만 캐시
 */
public interface ReferenceDataCache {

    /**
     * 출판사 이름으로 Publisher 엔티티 조회
     * 
     * @param publisherName 출판사 이름 (정규화 전 원본)
     * @return Publisher 엔티티 또는 null
     */
    Publisher findPublisher(String publisherName);

    /**
     * 캐시에 Publisher 추가
     * 
     * @param name 출판사 이름 (키)
     * @param publisher Publisher 엔티티
     */
    void putPublisher(String name, Publisher publisher);

    /**
     * Repository에서 전체 데이터를 조회해 캐시 구축
     * 
     * @param publisherRepository Publisher 조회용 Repository
     */
    void buildFromRepository(PublisherRepository publisherRepository);

    /**
     * 캐시 초기화
     */
    void clear();

    /**
     * 캐시 준비 완료 여부 확인
     * 
     * @return 캐시 구축 완료 여부
     */
    boolean isReady();

    /**
     * 캐시된 Publisher 수 반환
     * 
     * @return Publisher 캐시 크기
     */
    int getPublisherCacheSize();
}
