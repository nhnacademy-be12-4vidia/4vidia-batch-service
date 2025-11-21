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
public class GoogleBookItemDto {

    @JsonProperty("volumeInfo")
    private VolumeInfoDto volumeInfo;

    @JsonProperty("saleInfo")
    private SaleInfoDto saleInfo;

}
