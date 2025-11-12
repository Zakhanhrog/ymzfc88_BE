package com.xsecret.security;

import com.xsecret.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long id;
    private String username;
    private String email;
    private String password;
    private User.Role role;
    private User.StaffRole staffRole;
    private User.UserStatus status;

    public static UserPrincipal create(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getStaffRole(),
                user.getStatus()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        if (staffRole != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + staffRole.name()));
            if (staffRole == User.StaffRole.AGENT) {
                authorities.add(new SimpleGrantedAuthority("ROLE_AGENT_PORTAL"));
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_STAFF_PORTAL"));
            }
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != User.UserStatus.SUSPENDED && status != User.UserStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == User.UserStatus.ACTIVE;
    }
}
