package edu.agile.sis.security;

public final class Security {
    private Security(){}

    public static boolean hasRole(String role){
        return AuthSession.getInstance().hasRole(role);
    }

    public static void requireRole(String role){
        if (!hasRole(role)) {
            throw new SecurityException("User requires role: " + role);
        }
    }
}
