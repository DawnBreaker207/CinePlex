package com.dawn.common.infra.security;

import com.dawn.common.core.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("securityPolicy")
public class SecurityPolicy {

    private static boolean ENABLED = true;

    @Value("${app.security.enabled:true}")
    public void setEnabled(boolean enabled) {
        SecurityPolicy.ENABLED = enabled;
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public boolean hasRole(String role) {
        if (!ENABLED) return true;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role));
    }

    public boolean hasAnyRole(String... roles) {
        if (!ENABLED) return true;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        var authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        for (String role : roles) {
            if (authorities.contains("ROLE_" + role)) return true;
        }
        return false;
    }

    public Long requireAuthenticated() {
        if (!ENABLED) return 1L;
        Long uid = SecurityUtils.getCurrentUserId();
        if (uid == null) throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        return uid;
    }
}
