package init.data.DataParser.service;

import init.data.DataParser.DTO.ParsingDto;
import init.data.DataParser.entity.Author;
import init.data.DataParser.entity.Book;
import init.data.DataParser.entity.BookAuthor;
import init.data.DataParser.entity.Publisher;
import init.data.DataParser.parser.DataParser;
import init.data.DataParser.parser.DataParserResolver;
import java.io.File;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataParsingService {

    private final BookService bookService;
    private final AuthorService authorService;
    private final BookAuthorService bookAuthorService;
    private final PublisherService publisherService;
    private final DataParserResolver dataParserResolver;


    //  @PostConstruct
    public void init() {
        try {
            loadData("classpath:data/*.*");
        } catch (IOException e) {
            log.error("데이터 로드 실패", e);
        }

    }

    public void loadData(String location) throws IOException {
        PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

        Resource[] resources = resourceResolver.getResources(location);

        for (Resource r : resources) {
            String fileName = r.getFilename();
            if (fileName == null) {
                continue;
            }

            DataParser parser = dataParserResolver.getDataParser(fileName);

            if (parser != null) {
                File file = r.getFile();
                List<ParsingDto> records = parser.parsing(file);
                saveData(records);
            } else {
                log.error("지원하는 parser가 없습니다.");
            }
        }
    }

    private void saveData(List<ParsingDto> records) throws IOException {
        for (ParsingDto dto : records) {
            try {
                Book book;
                Author author;
                List<String> authorList = dto.getAuthors();

                book = bookService.create(dto);

                for (String name : authorList) {
                    author = authorService.save(name, book.getLongIsbn());
                    BookAuthor bookAuthor = bookAuthorService.save(book, author);
                }
                Publisher publisher = publisherService.save(dto);
            } catch (IllegalArgumentException e) {
                log.error("무언가 잘못되었습니다.");
            } catch (Exception e) {
                log.error("무언가 완전히 잘못되었습니다.");
            }
        }
    }


}
