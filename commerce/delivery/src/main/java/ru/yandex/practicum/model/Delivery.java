package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.DeliveryState;

import java.util.UUID;

@Entity
@Table(name = "deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {
    @Id
    private UUID deliveryId;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private String fromCountry;

    @Column(nullable = false)
    private String fromCity;

    @Column(nullable = false)
    private String fromStreet;

    @Column(nullable = false)
    private String fromHouse;

    private String fromFlat;

    @Column(nullable = false)
    private String toCountry;

    @Column(nullable = false)
    private String toCity;

    @Column(nullable = false)
    private String toStreet;

    @Column(nullable = false)
    private String toHouse;

    private String toFlat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DeliveryState deliveryState = DeliveryState.CREATED;
}