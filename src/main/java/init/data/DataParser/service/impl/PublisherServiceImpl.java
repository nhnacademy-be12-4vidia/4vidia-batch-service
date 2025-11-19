package init.data.DataParser.service.impl;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Publisher;
import init.data.DataParser.repository.PublisherRepository;
import init.data.DataParser.service.PublisherService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PublisherServiceImpl implements PublisherService {

    private final PublisherRepository publisherRepository;

    @Override
    public Publisher getByName(String publisherName) {
        if (isExists(publisherName)) {
            return publisherRepository.findByName(publisherName);
        }
        throw new IllegalArgumentException("등록되지 않은 출판사입니다.");
    }

    @Override
    public Publisher createByApi(Publisher publisher) {
        if (publisherRepository.existsByName(publisher.getName())) {
            throw new IllegalArgumentException("이미 등록된 출판사입니다.");
        }
        return publisherRepository.save(publisher);
    }

    @Override
    public List<Publisher> saveAll(Iterable<Publisher> publishers) {
        return publisherRepository.saveAll(publishers);
    }

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
