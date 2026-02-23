package com.example.usermanagement.model.enums;

/**
 * Defines the roles a user can hold within the system.
 * Stored as a string in the database for readability and schema stability.
 */
public enum Role {
    USER,
    MODERATOR,
    ADMIN
}
