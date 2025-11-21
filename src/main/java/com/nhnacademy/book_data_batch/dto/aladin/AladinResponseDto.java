package com.nhnacademy.book_data_batch.dto.aladin;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AladinResponseDto {
    private List<AladinItemDto> item;

}
