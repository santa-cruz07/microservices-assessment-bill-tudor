package com.sc07.commerce.order.entity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderCreate(
        @NotBlank @Email String customerEmail,
        @NotBlank String itemDescription,
        @Min(1) Integer quantity) {

}
