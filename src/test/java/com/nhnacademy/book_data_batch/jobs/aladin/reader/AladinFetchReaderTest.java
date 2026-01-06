package com.nhnacademy.book_data_batch.jobs.aladin.reader;

import com.nhnacademy.book_data_batch.infrastructure.client.aladin.AladinApiClient;
import com.nhnacademy.book_data_batch.infrastructure.client.aladin.AladinQuotaTracker;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.api.AladinItemDto;
import com.nhnacademy.book_data_batch.jobs.aladin.dto.api.AladinResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AladinFetchReaderTest {

    @Mock
    private AladinApiClient aladinApiClient;

    @Mock
    private AladinQuotaTracker aladinQuotaTracker;

    private AladinFetchReader reader;
    private List<String> apiKeys = List.of("key1");

    @Test
    @DisplayName("재시작 시 저장된 상태 복원 테스트")
    void testRestartability() throws Exception {
        // Given
        reader = new AladinFetchReader(aladinApiClient, aladinQuotaTracker, apiKeys);
        
        // Mocking: tryAcquire 항상 성공
        when(aladinQuotaTracker.tryAcquire(anyString())).thenReturn(true);

        // Mocking: API 응답 설정 (1페이지, 2페이지)
        AladinItemDto item1 = mock(AladinItemDto.class);
        AladinResponseDto response1 = new AladinResponseDto(100, 1, 10, List.of(item1), null, null);
        
        AladinItemDto item2 = mock(AladinItemDto.class);
        AladinResponseDto response2 = new AladinResponseDto(100, 2, 10, List.of(item2), null, null);

        // 첫 번째 실행: 1페이지 호출
        when(aladinApiClient.listItems(1, "key1")).thenReturn(Optional.of(response1));
        
        // When: 첫 번째 읽기 (1페이지)
        reader.read(); 

        // 상태 저장
        ExecutionContext executionContext = new ExecutionContext();
        reader.update(executionContext);

        // Then: 상태 검증 (다음 페이지는 2가 되어야 함 - read() 호출 시 currentPage++ 되므로)
        assertThat(executionContext.getInt("aladin.fetch.currentPage")).isEqualTo(2);

        // --- 재시작 시뮬레이션 ---
        
        // 새로운 Reader 생성
        AladinFetchReader newReader = new AladinFetchReader(aladinApiClient, aladinQuotaTracker, apiKeys);
        
        // Mocking: 2페이지 호출
        when(aladinApiClient.listItems(2, "key1")).thenReturn(Optional.of(response2));

        // 상태 복원
        newReader.open(executionContext);

        // 재시작 후 읽기 (2페이지 요청해야 함)
        newReader.read();

        // 검증: listItems가 2페이지로 호출되었는지 확인
        verify(aladinApiClient).listItems(2, "key1");
    }
    
    @Test
    @DisplayName("버퍼에 아이템이 남아있을 때 상태 저장 검증 - 페이지 보정 확인")
    void testUpdateStateWithRemainingBuffer() throws Exception {
        // Given
        reader = new AladinFetchReader(aladinApiClient, aladinQuotaTracker, apiKeys);
        when(aladinQuotaTracker.tryAcquire(anyString())).thenReturn(true);

        // API가 1페이지에 2개의 아이템을 반환하도록 설정
        AladinItemDto item1 = mock(AladinItemDto.class);
        AladinItemDto item2 = mock(AladinItemDto.class);
        AladinResponseDto response = new AladinResponseDto(100, 1, 10, List.of(item1, item2), null, null);
        when(aladinApiClient.listItems(1, "key1")).thenReturn(Optional.of(response));

        // When
        reader.read(); // 첫 번째 아이템만 읽음 (버퍼에 item2 남음)

        ExecutionContext executionContext = new ExecutionContext();
        reader.update(executionContext);

        // Then
        // 버퍼에 데이터가 남았으므로, 재시작 시 1페이지부터 다시 읽어야 함 (중복은 INSERT IGNORE가 처리)
        // 현재 로직은 여기서 2를 저장하여 실패할 것으로 예상됨
        assertThat(executionContext.getInt("aladin.fetch.currentPage"))
                .as("버퍼에 데이터가 남아있으면 현재 페이지(1)가 저장되어야 함")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("처음 시작 시 상태")
    void testFirstStart() throws Exception {
        reader = new AladinFetchReader(aladinApiClient, aladinQuotaTracker, apiKeys);
        ExecutionContext executionContext = new ExecutionContext();
        
        reader.open(executionContext);
        
        // Mocking
        when(aladinQuotaTracker.tryAcquire(anyString())).thenReturn(true);
        AladinItemDto item = mock(AladinItemDto.class);
        AladinResponseDto response = new AladinResponseDto(100, 1, 10, List.of(item), null, null);
        when(aladinApiClient.listItems(1, "key1")).thenReturn(Optional.of(response));
        
        reader.read();
        
        verify(aladinApiClient).listItems(1, "key1");
    }
}
