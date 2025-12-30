package com.nhnacademy.book_data_batch.jobs.discount_reprice.writer;

import com.nhnacademy.book_data_batch.jobs.discount_reprice.dto.DiscountRepriceTarget;
import com.nhnacademy.book_data_batch.infrastructure.jdbc.JdbcExecutor;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@Slf4j
public class DiscountRepriceItemWriter implements ItemWriter<DiscountRepriceTarget> {
    private static final String UPDATE_SQL = "UPDATE book SET price_sales = ? WHERE book_id = ?";

    private final JdbcExecutor jdbcExecutor;

    public DiscountRepriceItemWriter(JdbcExecutor jdbcExecutor) {
        this.jdbcExecutor = jdbcExecutor;
    }

    @Override
    public void write(Chunk<? extends DiscountRepriceTarget> chunk) throws Exception {
        List<? extends DiscountRepriceTarget> items = chunk.getItems();
        if (items.isEmpty()) {
            return;
        }
        jdbcExecutor.execute(
                UPDATE_SQL,
                items,
                (ps, item) -> {
                    ps.setObject(1, item.priceSales());
                    ps.setObject(2, item.bookId());
                },
                1000
        );
    }
}
