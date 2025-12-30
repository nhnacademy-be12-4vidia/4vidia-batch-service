package com.nhnacademy.book_data_batch.infrastructure.jdbc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.PreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JdbcExecutor 테스트")
class JdbcExecutorTest {

    private JdbcExecutor jdbcExecutor;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcExecutor = new JdbcExecutor(jdbcTemplate);
    }

    // ========== execute (배치 INSERT/UPDATE) 테스트 ==========

    @Test
    @DisplayName("execute: null 컬렉션일 때 아무것도 실행되지 않음")
    void execute_nullCollection_noExecution() {
        String sql = "INSERT INTO test VALUES (?, ?)";
        JdbcExecutor.PreparedStatementSetter<String> setter = (ps, item) -> {
        };

        jdbcExecutor.execute(sql, null, setter);

        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("execute: 빈 컬렉션일 때 아무것도 실행되지 않음")
    void execute_emptyCollection_noExecution() {
        String sql = "INSERT INTO test VALUES (?, ?)";
        JdbcExecutor.PreparedStatementSetter<String> setter = (ps, item) -> {
        };

        jdbcExecutor.execute(sql, new ArrayList<>(), setter);

        verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("execute: 배치 크기보다 작은 항목은 1번 실행")
    void execute_itemsLessThanBatchSize_executesOnce() {
        String sql = "INSERT INTO test VALUES (?)";
        List<String> items = Arrays.asList("item1", "item2", "item3");
        JdbcExecutor.PreparedStatementSetter<String> setter = (ps, item) -> ps.setString(1, item);

        when(jdbcTemplate.batchUpdate(eq(sql), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1, 1, 1});

        jdbcExecutor.execute(sql, items, setter, 10000);

        verify(jdbcTemplate, times(1)).batchUpdate(eq(sql), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("execute: 배치 크기를 초과하는 항목은 배치로 분할 실행")
    void execute_itemsExceedBatchSize_splitIntoBatches() {
        String sql = "INSERT INTO test VALUES (?)";
        List<String> items = new ArrayList<>();
        for (int i = 0; i < 25000; i++) {
            items.add("item" + i);
        }
        JdbcExecutor.PreparedStatementSetter<String> setter = (ps, item) -> ps.setString(1, item);

        when(jdbcTemplate.batchUpdate(eq(sql), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{10000});

        jdbcExecutor.execute(sql, items, setter, 10000);

        // 25000 items with batch size 10000 = 3 batches (10000 + 10000 + 5000)
        verify(jdbcTemplate, times(3)).batchUpdate(eq(sql), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("execute: 기본 배치 크기 사용")
    void execute_defaultBatchSize_uses10000() {
        String sql = "INSERT INTO test VALUES (?)";
        List<String> items = new ArrayList<>();
        for (int i = 0; i < 15000; i++) {
            items.add("item" + i);
        }
        JdbcExecutor.PreparedStatementSetter<String> setter = (ps, item) -> ps.setString(1, item);

        when(jdbcTemplate.batchUpdate(eq(sql), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{10000});

        jdbcExecutor.execute(sql, items, setter);

        // 15000 items with default batch size 1000 = 15 batches
        verify(jdbcTemplate, times(15)).batchUpdate(eq(sql), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("execute: Collection을 List로 변환하여 처리")
    void execute_collection_convertedToList() {
        String sql = "INSERT INTO test VALUES (?)";
        List<String> itemList = Arrays.asList("item1", "item2");
        JdbcExecutor.PreparedStatementSetter<String> setter = (ps, item) -> ps.setString(1, item);

        when(jdbcTemplate.batchUpdate(eq(sql), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{2});

        // HashSet을 Collection으로 전달
        jdbcExecutor.execute(sql, itemList, setter);

        verify(jdbcTemplate, times(1)).batchUpdate(eq(sql), any(BatchPreparedStatementSetter.class));
    }

    @Test
    @DisplayName("execute: PreparedStatement setter 올바르게 호출")
    void execute_preparedStatementSetterCalled() throws SQLException {
        String sql = "INSERT INTO test VALUES (?, ?)";
        List<String> items = Arrays.asList("test1", "test2");
        
        JdbcExecutor.PreparedStatementSetter<String> setter = mock(JdbcExecutor.PreparedStatementSetter.class);
        PreparedStatement ps = mock(PreparedStatement.class);

        when(jdbcTemplate.batchUpdate(eq(sql), any(BatchPreparedStatementSetter.class)))
                .thenAnswer(invocation -> {
                    BatchPreparedStatementSetter batchSetter = invocation.getArgument(1);
                    batchSetter.setValues(ps, 0);
                    batchSetter.setValues(ps, 1);
                    return new int[]{1, 1};
                });

        jdbcExecutor.execute(sql, items, setter);

        verify(setter, times(2)).setValues(eq(ps), any(String.class));
    }

    // ========== executeUpdate (단일 UPDATE/DELETE) 테스트 ==========

    @Test
    @DisplayName("executeUpdate: 영향받은 행 수 반환")
    void executeUpdate_returnsAffectedRowCount() {
        String sql = "UPDATE test SET name = ? WHERE id = ?";
        JdbcExecutor.PreparedStatementParameterSetter setter = ps -> {
            ps.setString(1, "updated");
            ps.setInt(2, 1);
        };

        when(jdbcTemplate.update(eq(sql), any(PreparedStatementSetter.class))).thenReturn(1);

        int result = jdbcExecutor.executeUpdate(sql, setter);

        assertEquals(1, result);
        verify(jdbcTemplate, times(1)).update(eq(sql), any(PreparedStatementSetter.class));
    }

    @Test
    @DisplayName("executeUpdate: 0개 행 업데이트")
    void executeUpdate_affectsNoRows() {
        String sql = "UPDATE test SET name = ? WHERE id = ?";
        JdbcExecutor.PreparedStatementParameterSetter setter = ps -> {
            ps.setString(1, "updated");
            ps.setInt(2, 999);
        };

        when(jdbcTemplate.update(eq(sql), any(PreparedStatementSetter.class))).thenReturn(0);

        int result = jdbcExecutor.executeUpdate(sql, setter);

        assertEquals(0, result);
    }

    // ========== query (단순 조회) 테스트 ==========

    @Test
    @DisplayName("query: RowMapper를 사용하여 결과 매핑")
    void query_mapsResultsWithRowMapper() {
        String sql = "SELECT * FROM test WHERE id = ?";
        List<String> expected = Arrays.asList("result1", "result2");

        when(jdbcTemplate.query(eq(sql), any(RowMapper.class), eq(1)))
                .thenReturn(expected);

        List<String> result = jdbcExecutor.query(sql, (rs, rowNum) -> "result" + (rowNum + 1), 1);

        assertEquals(expected, result);
        verify(jdbcTemplate, times(1)).query(eq(sql), any(RowMapper.class), eq(1));
    }

    @Test
    @DisplayName("query: 빈 결과 반환")
    void query_returnsEmptyList() {
        String sql = "SELECT * FROM test WHERE id = ?";

        when(jdbcTemplate.query(eq(sql), any(RowMapper.class), eq(999)))
                .thenReturn(new ArrayList<>());

        List<String> result = jdbcExecutor.query(sql, (rs, rowNum) -> "result", 999);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("query: 여러 인자 전달")
    void query_passesMultipleArguments() {
        String sql = "SELECT * FROM test WHERE id = ? AND name = ?";
        List<String> expected = Arrays.asList("result");

        when(jdbcTemplate.query(eq(sql), any(RowMapper.class), eq(1), eq("test")))
                .thenReturn(expected);

        List<String> result = jdbcExecutor.query(sql, (rs, rowNum) -> "result", 1, "test");

        assertEquals(expected, result);
        verify(jdbcTemplate, times(1)).query(eq(sql), any(RowMapper.class), eq(1), eq("test"));
    }

    // ========== queryInBatches (IN 절 배치 분할 조회) 테스트 ==========

    @Test
    @DisplayName("queryInBatches: null 이름 리스트일 때 빈 리스트 반환")
    void queryInBatches_nullNames_returnsEmptyList() {
        String sqlTemplate = "SELECT * FROM test WHERE name IN (%s)";
        List<String> result = jdbcExecutor.queryInBatches(sqlTemplate, (rs, rowNum) -> "result", null, 10);

        assertTrue(result.isEmpty());
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any());
    }

    @Test
    @DisplayName("queryInBatches: 빈 이름 리스트일 때 빈 리스트 반환")
    void queryInBatches_emptyNames_returnsEmptyList() {
        String sqlTemplate = "SELECT * FROM test WHERE name IN (%s)";
        List<String> result = jdbcExecutor.queryInBatches(sqlTemplate, (rs, rowNum) -> "result", new ArrayList<>(), 10);

        assertTrue(result.isEmpty());
        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any());
    }

    @Test
    @DisplayName("queryInBatches: 배치를 초과하는 항목은 분할 조회")
    void queryInBatches_itemsExceedBatchSize_splitIntoBatches() {
        String sqlTemplate = "SELECT * FROM test WHERE name IN (%s)";
        List<String> names = Arrays.asList("A", "B", "C");
        int batchSize = 2;

        // Mock for first batch (A, B)
        String sql1 = String.format(sqlTemplate, "?,?");
        when(jdbcTemplate.query(eq(sql1), any(RowMapper.class), eq("A"), eq("B")))
                .thenReturn(Arrays.asList("ResultA", "ResultB"));

        // Mock for second batch (C)
        String sql2 = String.format(sqlTemplate, "?");
        when(jdbcTemplate.query(eq(sql2), any(RowMapper.class), eq("C")))
                .thenReturn(Arrays.asList("ResultC"));

        List<String> result = jdbcExecutor.queryInBatches(sqlTemplate, (rs, rowNum) -> "ignored", names, batchSize);

        assertEquals(3, result.size());
        assertEquals(Arrays.asList("ResultA", "ResultB", "ResultC"), result);

        verify(jdbcTemplate).query(eq(sql1), any(RowMapper.class), eq("A"), eq("B"));
        verify(jdbcTemplate).query(eq(sql2), any(RowMapper.class), eq("C"));
    }



    // ========== 함수형 인터페이스 테스트 ==========

    @Test
    @DisplayName("PreparedStatementSetter: SQLException을 처리")
    void preparedStatementSetter_handlesException() {
        JdbcExecutor.PreparedStatementSetter<String> setter = (ps, item) -> {
            throw new SQLException("Test exception");
        };

        assertThrows(SQLException.class, () -> {
            PreparedStatement ps = mock(PreparedStatement.class);
            setter.setValues(ps, "test");
        });
    }

    @Test
    @DisplayName("PreparedStatementParameterSetter: SQLException을 처리")
    void preparedStatementParameterSetter_handlesException() {
        JdbcExecutor.PreparedStatementParameterSetter setter = ps -> {
            throw new SQLException("Test exception");
        };

        assertThrows(SQLException.class, () -> {
            PreparedStatement ps = mock(PreparedStatement.class);
            setter.setValues(ps);
        });
    }
}
