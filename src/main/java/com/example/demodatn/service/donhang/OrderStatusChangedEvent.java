package com.example.demodatn.service.donhang;

public record OrderStatusChangedEvent(
        Integer orderId,
        String status,
        String note,
        String carrier,
        String trackingCode
) {}
