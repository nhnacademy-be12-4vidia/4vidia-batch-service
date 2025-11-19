package init.data.DataParser.DTO.Google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class SaleInfoDto {

    @JsonProperty("listPrice")
    private PriceDto listPrice;

    @JsonProperty("retailPrice")
    private PriceDto retailPrice;
}
