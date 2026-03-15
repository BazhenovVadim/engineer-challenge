package com.vadim.grpc.authservice.domain.model;

import java.util.regex.Pattern;

public class PasswordPolicy {
    private static final Pattern RULE = Pattern.compile("^(?=.*[0-9])(?=.*[A-Z]).{8,}$");

    public static boolean validate(String password) {
        if (password == null) return false;
        return RULE.matcher(password).matches();
    }
}