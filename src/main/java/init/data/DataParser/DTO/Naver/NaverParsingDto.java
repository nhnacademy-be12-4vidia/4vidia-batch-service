package init.data.DataParser.DTO.Naver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class NaverParsingDto {

    private String title;
    private String author;
    private String price;
    private String discount;
    private String publisher;
    private String pubDate;
    private String isbn;
    private String image;
    private String description;

}