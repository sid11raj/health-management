package com.healthrecords.security;

/**
 * Password policy, enforced in AuthService (server-side, so a client cannot
 * bypass it) and mirrored as a hint in the frontend register form.
 * Minimum 12 characters with at least one upper-case, one lower-case,
 * one digit and one symbol. BCrypt hashing itself is unchanged.
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 12;

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > 100) {
            return false;
        }
        boolean upper = false, lower = false, digit = false, symbol = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) upper = true;
            else if (Character.isLowerCase(c)) lower = true;
            else if (Character.isDigit(c)) digit = true;
            else symbol = true;
        }
        return upper && lower && digit && symbol;
    }

    public static String requirementMessage() {
        return "Password must be at least 12 characters and contain an upper-case letter, "
                + "a lower-case letter, a digit and a symbol.";
    }
}
