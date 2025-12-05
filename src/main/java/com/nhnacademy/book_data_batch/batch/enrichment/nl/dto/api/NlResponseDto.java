package com.nhnacademy.book_data_batch.batch.enrichment.nl.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NlResponseDto(

) {
}
