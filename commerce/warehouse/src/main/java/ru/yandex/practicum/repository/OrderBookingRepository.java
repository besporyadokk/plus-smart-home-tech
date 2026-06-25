package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.OrderBooking;

import java.util.Optional;
import java.util.UUID;

public interface OrderBookingRepository extends JpaRepository<OrderBooking, UUID> {
    Optional<OrderBooking> findByOrderId(UUID orderId);
    boolean existsByOrderId(UUID orderId);
}