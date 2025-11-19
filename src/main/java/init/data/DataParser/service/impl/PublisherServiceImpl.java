package init.data.DataParser.service.impl;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Publisher;
import init.data.DataParser.repository.PublisherRepository;
import init.data.DataParser.service.PublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {

    private final PublisherRepository publisherRepository;

    @Override
    public Publisher save(ParsingDto dto) {
        if (isExists(dto.getPublisher())) {
            throw new IllegalArgumentException();
        }

        Publisher publisher = new Publisher();
        publisher.setName(dto.getPublisher());
        return publisherRepository.save(publisher);
    }

    @Override
    public Boolean isExists(String name) {
        return publisherRepository.existsByName(name);
    }
}
