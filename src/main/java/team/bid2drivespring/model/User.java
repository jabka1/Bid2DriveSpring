package team.bid2drivespring.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.time.LocalDate;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = true, unique = true)
    private String passportNumber;

    @Column(nullable = true)
    private String verificationPhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.NOT_SUBMITTED;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String verificationComment;

    @Column(nullable = true)
    private String firstName;

    @Column(nullable = true)
    private String lastName;

    @Column(nullable = true)
    private LocalDate dateOfBirth;

    @Column(nullable = true)
    private String country;

    @Column(nullable = true)
    private String city;

    @Column(nullable = true)
    private String profilePhotoUrl;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public enum Role {
        USER,
        ADMIN
    }

    public enum VerificationStatus {
        NOT_SUBMITTED,
        PENDING,
        APPROVED,
        REJECTED
    }

}
