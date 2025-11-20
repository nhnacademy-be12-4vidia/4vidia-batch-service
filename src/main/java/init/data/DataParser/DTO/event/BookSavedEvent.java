package init.data.DataParser.DTO.event;

import init.data.DataParser.entity.Book;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookSavedEvent {

    private List<Book> savedBooks;

}
