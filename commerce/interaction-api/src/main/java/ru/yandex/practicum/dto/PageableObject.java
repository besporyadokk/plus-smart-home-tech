package ru.yandex.practicum.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageableObject {
    private long offset;
    private SortObject sort;
    private boolean unpaged;
    private boolean paged;
    private int pageNumber;
    private int pageSize;
}