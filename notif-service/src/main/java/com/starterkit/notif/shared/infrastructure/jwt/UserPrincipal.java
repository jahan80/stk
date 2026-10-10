package com.starterkit.notif.shared.infrastructure.jwt;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final List<String> roles;
    private final List<String> permissions;
    private final boolean isAdmin;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(Long id, String username, String email,
                         List<String> roles, List<String> permissions) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles != null ? roles : List.of();
        this.permissions = permissions != null ? permissions : List.of();

        // Admin if role=ADMIN OR wildcard permission
        this.isAdmin = this.roles.contains("ADMIN")
                || this.permissions.contains("*")
                || this.permissions.contains("admin:*");

        List<GrantedAuthority> auths = new ArrayList<>();
        this.roles.forEach(r -> auths.add(new SimpleGrantedAuthority("ROLE_" + r)));
        this.permissions.forEach(p -> auths.add(new SimpleGrantedAuthority(p)));
        this.authorities = auths;
    }

    public boolean hasRole(String role) {
        return this.roles.contains(role);
    }

    public boolean hasPermission(String permission) {
        return this.permissions.contains(permission);
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
