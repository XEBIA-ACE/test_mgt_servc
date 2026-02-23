package com.example.usermanagement.model.entity;

/**
 * User roles used for method-level authorization.
 * Stored as strings in the database.
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN,
    ROLE_MODERATOR
}
