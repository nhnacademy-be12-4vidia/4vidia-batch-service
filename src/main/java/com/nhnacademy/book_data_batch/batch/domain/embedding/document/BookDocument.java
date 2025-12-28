package com.nhnacademy.book_data_batch.batch.domain.embedding.document;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.*;

@Document(indexName = "4vidia-books")
//@Document(indexName = "4vidia-books-test")
@Getter
@Builder(toBuilder = true)
public class BookDocument {

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

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "nori"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private List<String> tags;

    @Field(type = FieldType.Date, format = DateFormat.date, pattern = "yyyy-MM-dd")
    @Setter
    private LocalDate publishedDate;

    @Field(type = FieldType.Dense_Vector, dims = 1024)
    private double[] embedding;

    @Field(type = FieldType.Double)
    private Double rating;
}
