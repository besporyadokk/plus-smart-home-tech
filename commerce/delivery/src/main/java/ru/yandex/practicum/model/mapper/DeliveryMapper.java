package ru.yandex.practicum.model.mapper;

import lombok.experimental.UtilityClass;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.dto.DeliveryState;
import ru.yandex.practicum.model.Delivery;

import java.util.UUID;

@UtilityClass
public class DeliveryMapper {

    public static Delivery toEntity(DeliveryDto dto) {
        if (dto == null) {
            return null;
        }

        return Delivery.builder()
                .deliveryId(dto.getDeliveryId() != null ? dto.getDeliveryId() : UUID.randomUUID())
                .orderId(dto.getOrderId())
                .fromCountry(dto.getFromAddress().getCountry())
                .fromCity(dto.getFromAddress().getCity())
                .fromStreet(dto.getFromAddress().getStreet())
                .fromHouse(dto.getFromAddress().getHouse())
                .fromFlat(dto.getFromAddress().getFlat())
                .toCountry(dto.getToAddress().getCountry())
                .toCity(dto.getToAddress().getCity())
                .toStreet(dto.getToAddress().getStreet())
                .toHouse(dto.getToAddress().getHouse())
                .toFlat(dto.getToAddress().getFlat())
                .deliveryState(dto.getDeliveryState() != null ? dto.getDeliveryState() : DeliveryState.CREATED)
                .build();
    }

    public static DeliveryDto toDto(Delivery entity) {
        if (entity == null) {
            return null;
        }

        AddressDto fromAddress = AddressDto.builder()
                .country(entity.getFromCountry())
                .city(entity.getFromCity())
                .street(entity.getFromStreet())
                .house(entity.getFromHouse())
                .flat(entity.getFromFlat())
                .build();

        AddressDto toAddress = AddressDto.builder()
                .country(entity.getToCountry())
                .city(entity.getToCity())
                .street(entity.getToStreet())
                .house(entity.getToHouse())
                .flat(entity.getToFlat())
                .build();

        return DeliveryDto.builder()
                .deliveryId(entity.getDeliveryId())
                .orderId(entity.getOrderId())
                .fromAddress(fromAddress)
                .toAddress(toAddress)
                .deliveryState(entity.getDeliveryState())
                .build();
    }
}