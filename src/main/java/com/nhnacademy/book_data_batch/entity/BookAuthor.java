package com.nhnacademy.book_data_batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "book_author", indexes = {
    @Index(name = "idx_book_id", columnList = "book_id"),
    @Index(name = "idx_author_id", columnList = "author_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_book_author", columnNames = {"book_id", "author_id"})
})
@NoArgsConstructor
@AllArgsConstructor
public class BookAuthor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_author_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Setter
    private Book book;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Setter
    private Author author;

    @Column(name = "author_role")
    @Setter
    private String role;

    @Builder
    public BookAuthor(Book book, Author author, String role) {
        this.book = book;
        this.author = author;
        this.role = role;
    }
}
