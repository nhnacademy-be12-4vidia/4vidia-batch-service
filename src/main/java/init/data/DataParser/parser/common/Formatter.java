package init.data.DataParser.parser.common;

import java.time.format.DateTimeFormatter;

public class Formatter {

    public static final DateTimeFormatter NAVER_DATE_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyyMMdd");

    public static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyy-MM-dd");
}
