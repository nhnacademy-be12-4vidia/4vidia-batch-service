package init.data.DataParser.DTO.Aladin;

import init.data.DataParser.entity.Author;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AladinAuthorWithRoleDto {

    private Author author;
    private String role;

}
