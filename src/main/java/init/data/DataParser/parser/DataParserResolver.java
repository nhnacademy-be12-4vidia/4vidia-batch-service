package init.data.DataParser.parser;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataParserResolver {

  private final List<DataParser> dataParserList;

  public DataParser getDataParser(String fileName) {
    return dataParserList.stream()
        .filter(p -> p.matchFileType(fileName))
        .findFirst()
        .orElse(null);
  }

}
