package ru.yandex.practicum.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.dto.OrderState;

import java.util.UUID;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @Column(nullable = false)
    private UUID shoppingCartId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private UUID paymentId;

    @Column(nullable = false)
    private UUID deliveryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderState state = OrderState.NEW;

    @Column(nullable = false)
    private Double deliveryWeight;

    @Column(nullable = false)
    private Double deliveryVolume;

    @Column(nullable = false)
    private Boolean fragile;

    @Column(nullable = false)
    private Double totalPrice;

    @Column(nullable = false)
    private Double deliveryPrice;

    @Column(nullable = false)
    private Double productPrice;
}