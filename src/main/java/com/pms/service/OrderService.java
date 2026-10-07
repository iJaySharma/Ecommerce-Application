package com.pms.service;

import com.pms.dto.request.CheckoutRequest;
import com.pms.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse checkout(Long userId, CheckoutRequest request);
    List<OrderResponse> getOrdersForUser(Long userId);
    List<OrderResponse> getAllOrders();
}
