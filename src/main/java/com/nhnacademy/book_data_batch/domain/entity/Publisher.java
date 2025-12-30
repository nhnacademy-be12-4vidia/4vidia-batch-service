package com.nhnacademy.book_data_batch.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "publisher")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Publisher extends BaseEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "publisher_id")
    private Long id;

    @Column(name = "publisher_name", nullable = false, unique = true, length = 255)
    private String name;

    @Builder
    public Publisher(String name) {
        this.name = name;
    }
}
