package com.nhnacademy.book_data_batch.batch.book.reader;

import com.nhnacademy.book_data_batch.batch.book.dto.BookCsvRow;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

public class BookCsvFieldSetMapper implements FieldSetMapper<BookCsvRow> {

    @Override
    public BookCsvRow mapFieldSet(FieldSet fieldSet) throws BindException {
        return new BookCsvRow(
                fieldSet.readString("seqNo"),
                fieldSet.readString("isbn13"),
                fieldSet.readString("volumeNumber"),
                fieldSet.readString("title"),
                fieldSet.readString("author"),
                fieldSet.readString("publisher"),
                fieldSet.readString("publishedDate"),
                fieldSet.readString("editionSymbol"),
                fieldSet.readString("price"),
                fieldSet.readString("imageUrl"),
                fieldSet.readString("description"),
                fieldSet.readString("kdcCode"),
                fieldSet.readString("titleSearch"),
                fieldSet.readString("authorSearch"),
                fieldSet.readString("secondaryPublishedDate"),
                fieldSet.readString("internetBookstoreYn"),
                fieldSet.readString("portalSiteYn"),
                fieldSet.readString("isbn10")
        );
    }
}
