package org.openfamilycompass.model;

public enum UserRole {
    ADMIN,
    PARENT,
    CHILD;

    public String getDisplayKey() {
        return switch (this) {
            case ADMIN -> "role.admin";
            case PARENT -> "role.parent";
            case CHILD -> "role.child";
        };
    }
}
