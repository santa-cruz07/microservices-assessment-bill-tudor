package com.sc07.commerce.notification.configuration;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class ApiKeyAuthToken extends AbstractAuthenticationToken {

    private final TenantPrincipal principal;

    public ApiKeyAuthToken(TenantPrincipal principal) {
        super(List.of(new SimpleGrantedAuthority("ROLE_TENANT")));
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public TenantPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return "";
    }
}