package team.bid2drivespring.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;


@Entity
@Data
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean activated = false;

    @Column(nullable = true)
    private String activationtoken;

    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    @Column(nullable = true)
    private long lockTime = 0;

    @Column(nullable = false)
    private boolean twoFactorEnabled = false;

    @Column(nullable = true)
    private String twoFactorCode;


    @Column(nullable = true)
    private String recoveryToken;

    @Column(nullable = true)
    private java.time.LocalDateTime tokenExpiryTime;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }
}