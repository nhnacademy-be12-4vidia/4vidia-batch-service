package init.data.DataParser.DTO.Aladin;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AladinResponseDto {
    private List<AladinItemDto> item;

}
