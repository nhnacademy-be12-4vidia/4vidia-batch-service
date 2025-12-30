package com.nhnacademy.book_data_batch.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "author")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Author extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "author_id")
    private Long id;

    @Column(name = "author_name", nullable = false, unique = true, length = 255,
            columnDefinition = "VARCHAR(255) DEFAULT '작자미상'")
    private String name;

    @Builder
    public Author(String name) {
        this.name = name;
        if (this.name == null || this.name.isBlank()) {
            this.name = "작자미상";
        }
    }
}
