package init.data.DataParser.DTO.Google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class VolumeInfoDto {

  @JsonProperty("title")
  private String title;

  @JsonProperty("subtitle")
  private String subtitle;

  @JsonProperty("publisher")
  private String publisher;

  @JsonProperty("publishedDate")
  private String publishedDate;

  @JsonProperty("description")
  private String description;

  @JsonProperty("pageCount")
  private Integer pageCount;

  @JsonProperty("language")
  private String language;

  @JsonProperty("imageLinks")
  private ImageLinksDto imageLinks;

  @JsonProperty("industryIdentifiers")
  private List<IndustryIdentifierDto> industryIdentifiers;

}
