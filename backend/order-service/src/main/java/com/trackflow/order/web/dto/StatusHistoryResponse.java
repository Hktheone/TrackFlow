package com.trackflow.order.web.dto;

import java.time.Instant;

import com.trackflow.order.domain.OrderStatus;
import com.trackflow.order.domain.OrderStatusHistory;
import com.trackflow.order.domain.StatusChangeSource;

public record StatusHistoryResponse(OrderStatus status, StatusChangeSource source, String note, Instant changedAt) {

    public static StatusHistoryResponse from(OrderStatusHistory entry) {
        return new StatusHistoryResponse(entry.getStatus(), entry.getSource(), entry.getNote(), entry.getChangedAt());
    }
}
