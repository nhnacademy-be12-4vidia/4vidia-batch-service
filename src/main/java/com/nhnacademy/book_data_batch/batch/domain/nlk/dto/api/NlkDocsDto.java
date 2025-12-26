package com.nhnacademy.book_data_batch.batch.domain.nlk.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NlkDocsDto(
        @JsonProperty("TITLE") String title,
        @JsonProperty("VOL") String vol,
        @JsonProperty("SERIES_TITLE") String seriesTitle,
        @JsonProperty("SERIES_NO") String seriesNo,
        @JsonProperty("AUTHOR") String author,
        @JsonProperty("EA_ISBN") String eaIsbn,
        @JsonProperty("EA_ADD_CODE") String eaAddCode,
        @JsonProperty("SET_ISBN") String setIsbn,
        @JsonProperty("SET_ADD_CODE") String setAddCode,
        @JsonProperty("SET_EXPRESSION") String setExpression,
        @JsonProperty("PUBLISHER") String publisher,
        @JsonProperty("EDITION_STMT") String editionStmt,
        @JsonProperty("PRE_PRICE") String prePrice,
        @JsonProperty("KDC") String kdc,
        @JsonProperty("DDC") String ddc,
        @JsonProperty("PAGE") String page,
        @JsonProperty("BOOK_SIZE") String bookSize,
        @JsonProperty("FORM") String form,
        @JsonProperty("PUBLISH_PREDATE") String publishPredate,
        @JsonProperty("SUBJECT") String subject,
        @JsonProperty("EBOOK_YN") String ebookYn,
        @JsonProperty("CIP_YN") String cipYn,
        @JsonProperty("CONTROL_NO") String controlNo,
        @JsonProperty("TITLE_URL") String titleUrl,
        @JsonProperty("BOOK_TB_CNT_URL") String bookTbCntUrl,
        @JsonProperty("BOOK_INTRODUCTION_URL") String bookIntroductionUrl,
        @JsonProperty("BOOK_SUMMARY_URL") String bookSummaryUrl,
        @JsonProperty("PUBLISHER_URL") String publisherUrl,
        @JsonProperty("INPUT_DATE") String inputDate,
        @JsonProperty("UPDATE_DATE") String updateDate
){
}