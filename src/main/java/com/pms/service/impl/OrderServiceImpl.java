package com.pms.service.impl;

import com.pms.dto.request.CheckoutRequest;
import com.pms.dto.response.AddressResponse;
import com.pms.dto.response.OrderItemResponse;
import com.pms.dto.response.OrderResponse;
import com.pms.entity.*;
import com.pms.enums.OrderStatus;
import com.pms.exception.EmptyCartException;
import com.pms.exception.InsufficientInventoryException;
import com.pms.exception.ResourceNotFoundException;
import com.pms.repository.*;
import com.pms.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Checkout flow:
     * 1. Validate cart is not empty (mandatory cart validation before payment).
     * 2. Validate the chosen address belongs to the user.
     * 3. For every cart line, lock the inventory row and confirm sufficient stock.
     *    If any line fails, the whole checkout fails and nothing is committed
     *    (the @Transactional boundary rolls back all changes).
     * 4. Deduct inventory, snapshot item prices, create Order + OrderItems.
     * 5. Clear the cart on success.
     */
    @Override
    @Transactional
    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new EmptyCartException("Cannot checkout with an empty cart");
        }

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", request.getAddressId()));

        if (!address.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Address does not belong to the current user");
        }

        Set<OrderItem> orderItems = new HashSet<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // Step 1: validate stock for every item first (fail fast, before mutating anything)
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();

            if (!product.isEnabled()) {
                throw new InsufficientInventoryException(
                        "Product '" + product.getName() + "' is currently disabled and cannot be ordered");
            }

            Inventory inventory = inventoryRepository.findByProductIdForUpdate(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", product.getId()));

            if (inventory.getQuantity() < cartItem.getQuantity()) {
                throw new InsufficientInventoryException(
                        "Insufficient inventory for product '" + product.getName() + "'. Requested: "
                                + cartItem.getQuantity() + ", Available: " + inventory.getQuantity());
            }
        }

        // Step 2: all validations passed -> deduct inventory and build order items
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            Inventory inventory = inventoryRepository.findByProductIdForUpdate(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory", "productId", product.getId()));

            inventory.setQuantity(inventory.getQuantity() - cartItem.getQuantity());
            inventoryRepository.save(inventory);

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(product.getPrice())
                    .build();
            orderItems.add(orderItem);
        }

        Order order = Order.builder()
                .user(user)
                .address(address)
                .totalAmount(totalAmount)
                .status(OrderStatus.PLACED)
                .build();

        orderItems.forEach(item -> item.setOrder(order));
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        // Step 3: clear the cart now that the order succeeded
        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();

        return toResponse(savedOrder);
    }

    @Override
    public List<OrderResponse> getOrdersForUser(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(oi -> OrderItemResponse.builder()
                        .productId(oi.getProduct().getId())
                        .productName(oi.getProduct().getName())
                        .quantity(oi.getQuantity())
                        .priceAtPurchase(oi.getPriceAtPurchase())
                        .subtotal(oi.getPriceAtPurchase().multiply(BigDecimal.valueOf(oi.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        AddressResponse addressResponse = AddressResponse.builder()
                .id(order.getAddress().getId())
                .line1(order.getAddress().getLine1())
                .line2(order.getAddress().getLine2())
                .city(order.getAddress().getCity())
                .state(order.getAddress().getState())
                .zipCode(order.getAddress().getZipCode())
                .country(order.getAddress().getCountry())
                .build();

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUser().getId())
                .username(order.getUser().getUsername())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .address(addressResponse)
                .items(items)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
