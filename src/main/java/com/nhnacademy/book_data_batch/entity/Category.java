package com.nhnacademy.book_data_batch.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "parent_category_id")
    @Setter
    private Category parentCategory;

    @Column(name = "kdc_code", nullable = false, unique = true, length = 3)
    @Setter
    private String kdcCode;

    @Column(name = "category_name", nullable = false, length = 70)
    @Setter
    private String name;

    // e.g. "/8/81/811/"
    @Column(name = "path", nullable = false, length = 9)
    @Setter
    private String path;

    @Column(name = "depth", nullable = false, columnDefinition = "TINYINT")
    @Setter
    private Integer depth;

    @Builder
    public Category(Category parentCategory, String kdcCode, String name, String path, Integer depth) {
        this.parentCategory = parentCategory;
        this.kdcCode = kdcCode;
        this.name = name;
        this.path = path;
        this.depth = depth;
    }
}
