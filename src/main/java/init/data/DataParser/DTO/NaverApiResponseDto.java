package init.data.DataParser.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class NaverApiResponseDto {

  private int total;
  private int start;
  private int display;
  private List<NaverParsingDto> items;

}
