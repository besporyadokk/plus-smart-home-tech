package ru.yandex.practicum.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sensors")
@Getter
@Setter
public class Sensor {
    @Id
    private String id;
    private String hubId;
}