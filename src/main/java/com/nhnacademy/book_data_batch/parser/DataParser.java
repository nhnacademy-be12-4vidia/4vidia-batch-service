package com.nhnacademy.book_data_batch.parser;

import com.nhnacademy.book_data_batch.dto.ParsingDto;
import java.io.File;
import java.io.IOException;
import java.util.List;

public interface DataParser {

    String getFileType();

    List<ParsingDto> parsing(File file) throws IOException;

    default boolean matchFileType(String fileName) {
        return fileName.trim().toLowerCase().endsWith(getFileType().toLowerCase());
    }

}
