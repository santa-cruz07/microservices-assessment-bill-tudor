package com.sc07.commerce.order.configuration;

import java.util.UUID;

public record TenantPrincipal(
        UUID tenantId
        //user emai address
) {
}
