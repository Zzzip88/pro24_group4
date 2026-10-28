package com.pro24_group4.pro24_group4.authorization;

public final class Authorities {

    private Authorities() {
    }

    // Roles
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";

    // User permissions
    public static final String USER_READ = "USER_READ";
    public static final String USER_WRITE = "USER_WRITE";
    public static final String USER_DELETE = "USER_DELETE";

    // Role permissions
    public static final String ROLE_READ = "ROLE_READ";
    public static final String ROLE_WRITE = "ROLE_WRITE";
    public static final String ROLE_DELETE = "ROLE_DELETE";

    // Permission permissions
    public static final String PERMISSION_READ = "PERMISSION_READ";
    public static final String PERMISSION_WRITE = "PERMISSION_WRITE";
    public static final String PERMISSION_DELETE = "PERMISSION_DELETE";
}
