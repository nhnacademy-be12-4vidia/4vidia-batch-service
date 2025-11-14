package init.data.DataParser.service;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Publisher;

public interface PublisherService {

  Publisher save(ParsingDto dto);

  Boolean isExists(String name);

}
