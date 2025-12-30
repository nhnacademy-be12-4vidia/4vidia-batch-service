package com.nhnacademy.book_data_batch.domain.service.author.parser.strategy;

import com.nhnacademy.book_data_batch.domain.service.author.parser.dto.ParsedAuthor;

import java.util.List;

public interface AuthorParsingStrategy {
    List<ParsedAuthor> parse(String input);
}
