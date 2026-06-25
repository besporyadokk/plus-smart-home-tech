package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.dto.PaymentStatus;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.feign.OrderClient;
import ru.yandex.practicum.feign.ShoppingStoreClient;
import ru.yandex.practicum.model.Payment;
import ru.yandex.practicum.repository.PaymentRepository;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ShoppingStoreClient shoppingStoreClient;
    private final OrderClient orderClient;

    @Override
    public Double calculateProductCost(OrderDto order) {
        log.debug("Calculating product cost for order: {}", order.getOrderId());

        double total = 0.0;

        for (Map.Entry<UUID, Integer> entry : order.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();

            ResponseEntity<ProductDto> response = shoppingStoreClient.getProduct(productId);
            ProductDto product = response.getBody();

            if (product == null) {
                throw new RuntimeException("Product not found: " + productId);
            }

            total += product.getPrice() * quantity;
        }

        log.debug("Product total: {}", total);
        return total;
    }

    @Override
    public Double calculateTotalCost(OrderDto order) {
        log.debug("Calculating total cost for order: {}", order.getOrderId());

        double productCost = calculateProductCost(order);
        double vat = productCost * 0.1;
        double deliveryCost = order.getDeliveryPrice() != null ? order.getDeliveryPrice() : 0.0;
        double total = productCost + vat + deliveryCost;

        log.debug("Product cost: {}, VAT: {}, Delivery: {}, Total: {}", productCost, vat, deliveryCost, total);
        return total;
    }

    @Override
    @Transactional
    public PaymentDto createPayment(OrderDto order) {
        log.debug("Creating payment for order: {}", order.getOrderId());

        double productTotal = calculateProductCost(order);
        double deliveryTotal = order.getDeliveryPrice() != null ? order.getDeliveryPrice() : 0.0;
        double feeTotal = productTotal * 0.1;
        double totalPayment = productTotal + feeTotal + deliveryTotal;

        Payment payment = Payment.builder()
                .paymentId(UUID.randomUUID())
                .orderId(order.getOrderId())
                .productTotal(productTotal)
                .deliveryTotal(deliveryTotal)
                .feeTotal(feeTotal)
                .totalPayment(totalPayment)
                .status(PaymentStatus.PENDING)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment created: {} for order: {}", saved.getPaymentId(), order.getOrderId());

        return PaymentDto.builder()
                .paymentId(saved.getPaymentId())
                .totalPayment(saved.getTotalPayment())
                .deliveryTotal(saved.getDeliveryTotal())
                .feeTotal(saved.getFeeTotal())
                .build();
    }

    @Override
    @Transactional
    public void paymentSuccess(UUID paymentId) {
        log.debug("Processing payment success: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        orderClient.payment(payment.getOrderId());
        log.info("Payment success for payment: {}, order: {}", paymentId, payment.getOrderId());
    }

    @Override
    @Transactional
    public void paymentFailed(UUID paymentId) {
        log.debug("Processing payment failed: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        orderClient.paymentFailed(payment.getOrderId());
        log.info("Payment failed for payment: {}, order: {}", paymentId, payment.getOrderId());
    }
}