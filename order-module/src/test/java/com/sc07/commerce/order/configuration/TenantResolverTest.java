package com.sc07.commerce.order.configuration;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantResolverTest {

    private final TenantResolver tenantResolver = new TenantResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesTenantIdFromSecurityContext() {
        UUID tenantId = UUID.randomUUID();
        SecurityContextHolder.getContext()
                .setAuthentication(new ApiKeyAuthToken(new TenantPrincipal(tenantId)));

        assertThat(tenantResolver.resolveCurrentTenantIdentifier()).isEqualTo(tenantId);
    }

    @Test
    void usesBootstrapTenantWhenSecurityContextIsMissing() {
        assertThat(tenantResolver.resolveCurrentTenantIdentifier())
                .isEqualTo(TenantResolver.BOOTSTRAP_TENANT_ID);
    }

    @Test
    void registersWithHibernate() {
        Map<String, Object> properties = new HashMap<>();

        tenantResolver.customize(properties);

        assertThat(properties)
                .containsEntry(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
    }
}
