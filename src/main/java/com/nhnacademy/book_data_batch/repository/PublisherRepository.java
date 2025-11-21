package com.nhnacademy.book_data_batch.repository;

import com.nhnacademy.book_data_batch.entity.Publisher;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublisherRepository extends JpaRepository<Publisher, Long> {

    Boolean existsByName(String name);

    Publisher findByName(String name);

    List<Publisher> findAllByNameIn(Collection<String> names);
}
