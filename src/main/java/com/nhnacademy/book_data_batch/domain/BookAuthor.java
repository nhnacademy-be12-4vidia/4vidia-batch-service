package com.nhnacademy.book_data_batch.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "book_author", indexes = {
    @Index(name = "idx_book_id", columnList = "book_id"),
    @Index(name = "idx_author_id", columnList = "author_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_book_author", columnNames = {"book_id", "author_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BookAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_author_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Column(name = "author_role")
    private String role;

    @Builder
    public BookAuthor(Book book, Author author, String role) {
        this.book = book;
        this.author = author;
        this.role = role;
    }
}
