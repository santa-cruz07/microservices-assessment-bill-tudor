package com.sc07.commerce.order.entity;

import com.sc07.commerce.order.entity.dto.OrderCreate;
import com.sc07.commerce.order.exception.IllegalOrderStatusException;
import com.sc07.commerce.order.exception.NoOpOrderStatusException;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.util.UUID;


/**
 * Simple Order Entity - customer email instead of spring-security oauth/keycloak implementation to save time
 * v2 I would extend to OneToMany -> OrderLines
 */

@Entity
@Table(name = "orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    //Typically this would just be a string referencing the oauth provider's id.
    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "item_description", nullable = false)
    private String itemDescription;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;



    public Order(@Valid OrderCreate request){
        this.customerEmail = request.customerEmail();
        this.itemDescription = request.itemDescription();
        this.quantity = request.quantity();
        this.status = OrderStatus.PENDING;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    /**
     * Check if a status change is legal. If allowed, return the previous status.
     * We can leverage previous against this.status to capture the delta
     *
     * @param target the desired new status
     * @return status before update
     */
    public OrderStatus updateStatus(OrderStatus target) {
        //No operation is not allowed
        //Possibly consider this simply an idempotent route.
        if(this.status == target) {
            throw new NoOpOrderStatusException(id, target);
        }

        //Rejected Transition
        if(!this.status.canTransition(target)) {
            throw new IllegalOrderStatusException(id, this.status, target);
        }

        OrderStatus previousStatus = this.status;
        this.status = target;
        return previousStatus;
    }
}
