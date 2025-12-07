package com.nhnacademy.book_data_batch.service.author.parser;

import com.nhnacademy.book_data_batch.service.author.parser.dto.ParsedAuthor;

import java.util.List;

public interface AuthorParser {
    List<ParsedAuthor> parse(String input);
}
