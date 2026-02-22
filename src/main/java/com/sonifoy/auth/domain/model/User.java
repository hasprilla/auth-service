package com.sonifoy.auth.domain.model;

import com.sonifoy.auth.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {
    @Id
    private Long id;
    private String email;
    private String name;
    private String password;
    @Column("profile_type")
    private String profileType;
    private boolean verified;
    @Column("verification_code")
    private String verificationCode;
    @Column("verification_code_expires_at")
    private LocalDateTime verificationCodeExpiresAt;
    @Column("avatar_url")
    private String avatarUrl;
    @Column("ip_address")
    private String ipAddress;
    private String city;
    private String country;
    @Column("device_data")
    private String deviceData;
    @Column("last_login_at")
    private LocalDateTime lastLoginAt;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Roles are handled as a Set of Strings, mapped to TEXT[] in Postgres via R2DBC
    @Column("roles")
    private Set<String> roles;
}
