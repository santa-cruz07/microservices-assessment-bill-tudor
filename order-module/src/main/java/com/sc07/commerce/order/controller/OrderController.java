package com.sc07.commerce.order.controller;

import com.sc07.commerce.order.configuration.TenantPrincipal;
import com.sc07.commerce.order.entity.Order;
import com.sc07.commerce.order.entity.dto.OrderCreate;
import com.sc07.commerce.order.entity.dto.OrderResponse;
import com.sc07.commerce.order.entity.dto.OrderStatusUpdate;
import com.sc07.commerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderCreate request) {
        Order order = orderService.create(request);
        return ResponseEntity
                .created(URI.create("/api/orders/" + order.getId()))
                .body(new OrderResponse(order));
    }

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable("id") UUID id) {
        return new OrderResponse(orderService.getById(id));
    }

    @GetMapping
    public List<OrderResponse> getAll() {
        return orderService.getAll().stream().map(OrderResponse::new).toList();
    }

    @PostMapping("/{id}/status")
    public OrderResponse changeStatus(@PathVariable("id") UUID id, @Valid @RequestBody OrderStatusUpdate request) {
        return new OrderResponse(orderService.changeStatus(id, request.status()));
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@PathVariable("id") UUID id) {
        return  new OrderResponse(orderService.cancel(id));
    }




}
