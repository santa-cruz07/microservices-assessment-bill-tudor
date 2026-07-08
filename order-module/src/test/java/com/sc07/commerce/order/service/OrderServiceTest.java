package com.sc07.commerce.order.service;

import com.sc07.commerce.order.entity.Order;
import com.sc07.commerce.order.entity.OrderStatus;
import com.sc07.commerce.order.entity.dto.OrderCreate;
import com.sc07.commerce.order.exception.OrderNotFoundException;
import com.sc07.commerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createBuildsPendingOrderAndPersistsIt() {
        Order saved = new Order(new OrderCreate("buyer@example.com", "Laptop", 1));
        saved.setId(UUID.randomUUID());
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        Order result = orderService.create(new OrderCreate("buyer@example.com", "Laptop", 1));

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(captor.getValue().getCustomerEmail()).isEqualTo("buyer@example.com");
        assertThat(result).isSameAs(saved);
    }


    @Test
    void getByIdThrowsWhenOrderDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getById(id))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void getAllReturnsAllRepositoryOrders() {
        List<Order> orders = List.of(
                new Order(new OrderCreate("one@example.com", "Keyboard", 1)),
                new Order(new OrderCreate("two@example.com", "Mouse", 2)));
        when(orderRepository.findAll()).thenReturn(orders);

        assertThat(orderService.getAll()).isEqualTo(orders);
    }

    @Test
    void changeStatusLoadsMutatesAndSavesOrder() {
        UUID id = UUID.randomUUID();
        Order order = new Order(new OrderCreate("buyer@example.com", "Laptop", 1));
        order.setId(id);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        Order result = orderService.changeStatus(id, OrderStatus.PROCESSING);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelTransitionsOrderToCancelled() {
        UUID id = UUID.randomUUID();
        Order order = new Order(new OrderCreate("buyer@example.com", "Laptop", 1));
        order.setId(id);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        assertThat(orderService.cancel(id).getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
}
