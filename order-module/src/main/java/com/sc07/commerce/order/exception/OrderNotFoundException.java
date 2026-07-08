package com.sc07.commerce.order.exception;

import java.util.UUID;

/**
 * Throws 404 so that 403 isn't exposed when a cross-tenant query is ran
 */
public class OrderNotFoundException extends RuntimeException {
  public OrderNotFoundException(UUID orderId) {
    super("Order %s not found".formatted(orderId));
  }
}
