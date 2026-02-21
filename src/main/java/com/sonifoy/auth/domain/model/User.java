package com.sonifoy.auth.domain.model;

import com.sonifoy.auth.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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
    private String profileType; // e.g. "LISTENER", "ARTIST", "LABEL"

    @Builder.Default
    private boolean isVerified = false;
    private String verificationCode;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Roles are handled differently in R2DBC vs JPA, often as a separate table or
    // JSON/Array column.
    // Simplifying here to just profileType first, or if using roles array in
    // Postgres
    private Set<String> roles;
}
