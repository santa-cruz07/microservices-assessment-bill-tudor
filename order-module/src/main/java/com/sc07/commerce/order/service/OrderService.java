package com.sc07.commerce.order.service;

import com.sc07.commerce.order.entity.Order;
import com.sc07.commerce.order.entity.OrderStatus;
import com.sc07.commerce.order.entity.dto.OrderCreate;
import com.sc07.commerce.order.exception.OrderNotFoundException;
import com.sc07.commerce.order.repository.OrderRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public Order create(@Valid OrderCreate request) {
        Order order = orderRepository.save(new Order(request));
        //append outbox
        return order;
    }

    @Transactional(readOnly = true)
    public Order getById(UUID id) {
        return orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    //Realistically this would have pagination passed to it.
    @Transactional(readOnly = true)
    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order changeStatus(UUID id, @NotNull OrderStatus status) {
        Order order = this.getById(id);
        OrderStatus previousStatus = order.updateStatus(status);
        log.debug("Order status changed from {} to {}", previousStatus, status);

        Order result = orderRepository.save(order);
        //append outbox
        return result;

    }

    @Transactional
    public Order cancel(UUID id) {
        return this.changeStatus(id, OrderStatus.CANCELLED);
    }


}
