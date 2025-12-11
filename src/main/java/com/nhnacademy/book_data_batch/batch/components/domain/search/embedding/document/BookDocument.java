package com.nhnacademy.book_data_batch.batch.components.domain.search.embedding.document;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "4vidia-books")
//@Document(indexName = "4vidia-books-test")
@Getter
@Builder(toBuilder = true)
public class BookDocument implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String title;

    @Field(type = FieldType.Keyword)
    private String isbn;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String description;

    @Field(type = FieldType.Keyword)
    private List<String> authors;

    @Field(type = FieldType.Keyword)
    private String publisher;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Integer)
    private Integer priceSales;

    @Field(type = FieldType.Text, analyzer = "nori")
    private List<String> tags;

    @Field(type = FieldType.Dense_Vector, dims = 1024)
    private double[] embedding;
}
