package init.data.DataParser.service;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Publisher;
import java.util.List;

public interface PublisherService {

    Publisher save(ParsingDto dto);

    Boolean isExists(String name);

    Publisher getByName(String publisherName);

    Publisher createByApi(Publisher publisher);

    List<Publisher> saveAll(Iterable<Publisher> publishers);

}
