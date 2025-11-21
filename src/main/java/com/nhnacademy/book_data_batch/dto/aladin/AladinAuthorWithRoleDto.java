package com.nhnacademy.book_data_batch.dto.aladin;

import com.nhnacademy.book_data_batch.entity.Author;
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
