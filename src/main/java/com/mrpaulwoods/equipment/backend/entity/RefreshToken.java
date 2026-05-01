package com.mrpaulwoods.equipment.backend.entity;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_token")
@AssociationOverride(name = "user", joinColumns = @JoinColumn(name = "user_id", nullable = false))
public class RefreshToken extends TokenEntity {
}
