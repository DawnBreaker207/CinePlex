package com.dawn.identity.model;

import com.dawn.common.core.model.AbstractMappedEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;


@Entity
@Table(name = "refresh_token")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class RefreshToken extends AbstractMappedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "expiry_date")
    private Instant expiryDate;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    // In-memory only (never persisted) — the value to put in the refresh cookie.
    @Transient
    private String rawToken;

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }
}
