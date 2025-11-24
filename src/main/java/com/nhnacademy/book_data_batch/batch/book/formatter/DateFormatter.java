package com.nhnacademy.book_data_batch.batch.book.formatter;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class DateFormatter {

    public static final List<DateTimeFormatter> PATTERNS = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyyMMdd"),
        DateTimeFormatter.BASIC_ISO_DATE
    );
}
