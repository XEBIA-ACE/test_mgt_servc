package com.example.usermanagement.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persisted refresh token that allows clients to obtain new access tokens
 * without re-authenticating. Tokens are single-use and rotation is enforced
 * on each refresh call.
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_token",   columnList = "token",   unique = true),
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** The opaque token value sent to and stored by the client. */
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Set to true when this token has been superseded or the user has logged out. */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /** A token is valid only when it is neither revoked nor past its expiry. */
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
