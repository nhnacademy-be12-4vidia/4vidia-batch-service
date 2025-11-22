package com.nhnacademy.book_data_batch.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "book_tag", indexes = {
    @Index(name = "idx_book_tag_book_id", columnList = "book_id"),
    @Index(name = "idx_book_tag_tag_id", columnList = "tag_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_book_tag", columnNames = {"book_id", "tag_id"})
})
@NoArgsConstructor
@AllArgsConstructor
public class BookTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_tag_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Setter
    private Book book;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Setter
    private Tag tag;

    @Builder
    public BookTag(Book book, Tag tag) {
        this.book = book;
        this.tag = tag;
    }
}
