package com.mrpaulwoods.equipment.backend.entity;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_token")
@AssociationOverride(name = "user", joinColumns = @JoinColumn(name = "user_id", nullable = false))
public class PasswordResetToken extends TokenEntity {
}
