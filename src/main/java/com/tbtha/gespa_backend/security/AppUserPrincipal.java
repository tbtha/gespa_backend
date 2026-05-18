package com.tbtha.gespa_backend.security;

import com.tbtha.gespa_backend.entities.Usuario;
import com.tbtha.gespa_backend.entities.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final boolean active;

    public AppUserPrincipal(Long id, String email, String passwordHash, UserRole role, boolean active) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public static AppUserPrincipal fromUsuario(Usuario usuario) {
        return fromUsuario(usuario, usuario.getRole());
    }

    public static AppUserPrincipal fromUsuario(Usuario usuario, UserRole role) {
        return new AppUserPrincipal(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getPasswordHash(),
                role,
                Boolean.TRUE.equals(usuario.getActive())
        );
    }

    public Long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
