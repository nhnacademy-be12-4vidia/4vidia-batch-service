package com.nhnacademy.book_data_batch.batch.enrichment.utils;

import java.util.ArrayList;
import java.util.List;

public class Partitioner {

    public static <T> List<List<T>> partition(List<T> list, int n) {
        List<List<T>> partitions = new ArrayList<>();
        int size = list.size();
        int partitionSize = (size + n - 1) / n;  // 올림 나눗셈

        for (int i = 0; i < n; i++) {
            int start = i * partitionSize;
            int end = Math.min(start + partitionSize, size);

            if (start < size) {
                partitions.add(new ArrayList<>(list.subList(start, end)));
            } else {
                partitions.add(new ArrayList<>());  // 빈 파티션
            }
        }

        return partitions;
    }
}
