package init.data.DataParser.DTO;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsingDto {

    Long longIsbn;
    String title;
    List<String> authors;
    String publisher;
    Long priceStandard;
    String imageUrl;
    String description;
    String publishedDate;
    Integer stock;
    Long shortIsbn;
}
