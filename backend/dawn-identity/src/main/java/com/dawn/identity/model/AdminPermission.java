package com.dawn.identity.model;

import com.dawn.common.core.constant.PermissionKey;
import com.dawn.common.core.model.AbstractMappedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "admin_permissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "permission_key"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AdminPermission extends AbstractMappedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_key", nullable = false, length = 20)
    private PermissionKey permissionKey;
}
