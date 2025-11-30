package com.nhnacademy.book_data_batch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "discount_policy", indexes = {
    @Index(name = "idx_discount_policy_category_id", columnList = "category_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DiscountPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_policy_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "discount_policy_name", nullable = false, length = 255)
    private String discountPolicyName;

    @Column(name = "discount_rate", nullable = false, columnDefinition = "TINYINT DEFAULT 10")
    private Integer discountRate = 10;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Builder
    public DiscountPolicy(Category category,
                          String discountPolicyName,
                          Integer discountRate,
                          LocalDate startDate,
                          LocalDate endDate) {
        this.category = category;
        this.discountPolicyName = discountPolicyName;
        this.discountRate = discountRate;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
