package com.nhnacademy.book_data_batch.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class IndustryIdentifierDto {

    @JsonProperty("type")
    private String type;

    @JsonProperty("identifier")
    private String identifier;
}
