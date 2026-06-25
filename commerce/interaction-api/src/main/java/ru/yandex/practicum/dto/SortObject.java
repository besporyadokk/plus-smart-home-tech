package ru.yandex.practicum.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SortObject {
    private String direction;
    private String nullHandling;
    private boolean ascending;
    private String property;
    private boolean ignoreCase;
}