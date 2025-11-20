package init.data.DataParser.repository.search;

import init.data.DataParser.document.BookDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, String> {

}
