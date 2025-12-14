package com.nhnacademy.book_data_batch.infrastructure.repository;

import com.nhnacademy.book_data_batch.domain.DiscountPolicy;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscountPolicyRepository extends JpaRepository<DiscountPolicy, Long> {

    @Query("SELECT dp FROM DiscountPolicy dp " +
           "WHERE dp.category.id IN :categoryIds " +
           "AND dp.startDate <= :asOfDate " +
           "AND dp.endDate >= :asOfDate")
    List<DiscountPolicy> findActivePolicies(@Param("categoryIds") Collection<Long> categoryIds,
                                            @Param("asOfDate") LocalDate asOfDate);
}
