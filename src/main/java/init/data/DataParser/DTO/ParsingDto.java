package init.data.DataParser.DTO;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsingDto {

    String isbn;
    String title;
    List<String> authors;
    String publisher;
    Integer priceStandard;
    String imageUrl;
    String description;
    String publishedDate;
    Integer stock;
}
