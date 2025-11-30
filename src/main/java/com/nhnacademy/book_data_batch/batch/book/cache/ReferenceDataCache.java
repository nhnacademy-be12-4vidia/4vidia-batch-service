package com.nhnacademy.book_data_batch.batch.book.cache;

import com.nhnacademy.book_data_batch.entity.Author;
import com.nhnacademy.book_data_batch.entity.Publisher;
import com.nhnacademy.book_data_batch.repository.AuthorRepository;
import com.nhnacademy.book_data_batch.repository.PublisherRepository;

/**
 * 참조 데이터(Author, Publisher) 캐시 인터페이스
 */
public interface ReferenceDataCache {

    /**
     * 작가 이름으로 Author 엔티티 조회
     * 
     * @param authorName 작가 이름 (정규화 전 원본)
     * @return Author 엔티티 또는 null
     */
    Author findAuthor(String authorName);

    /**
     * 출판사 이름으로 Publisher 엔티티 조회
     * 
     * @param publisherName 출판사 이름 (정규화 전 원본)
     * @return Publisher 엔티티 또는 null
     */
    Publisher findPublisher(String publisherName);

    /**
     * 캐시에 Author 추가
     * 
     * @param name 작가 이름 (키)
     * @param author Author 엔티티
     */
    void putAuthor(String name, Author author);

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
     * @param authorRepository Author 조회용 Repository
     * @param publisherRepository Publisher 조회용 Repository
     */
    void buildFromRepositories(AuthorRepository authorRepository, PublisherRepository publisherRepository);

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
     * 캐시된 Author 수 반환
     * 
     * @return Author 캐시 크기
     */
    int getAuthorCacheSize();

    /**
     * 캐시된 Publisher 수 반환
     * 
     * @return Publisher 캐시 크기
     */
    int getPublisherCacheSize();
}
