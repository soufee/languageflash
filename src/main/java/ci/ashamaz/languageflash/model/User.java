package ci.ashamaz.languageflash.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "confirmation_code")
    private String confirmationCode;

    @Column(name = "is_email_confirmed", nullable = false)
    private boolean emailConfirmed = false;

    @Column(name = "reset_code")
    private String resetCode;

    @Column(name = "reset_code_expiry")
    private LocalDateTime resetCodeExpiry;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new HashSet<>();

    @Column(name = "blocked", nullable = false)
    private boolean blocked = false;

    @Column(name = "is_premium", nullable = false)
    private boolean premium = false;

    @Column(name = "premium_expires_at")
    private LocalDateTime premiumExpiresAt;

    @Column(name = "interface_language", nullable = false)
    private String interfaceLanguage = "ru";

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "settings", columnDefinition = "text")
    private String settings;

    /** Premium активен, если флаг установлен и срок не истёк (бессрочный — при null-дате). */
    public boolean hasActivePremium() {
        return premium && (premiumExpiresAt == null || premiumExpiresAt.isAfter(LocalDateTime.now()));
    }
}
