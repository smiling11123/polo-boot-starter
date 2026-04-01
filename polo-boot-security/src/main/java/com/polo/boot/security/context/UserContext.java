package com.polo.boot.security.context;

import com.polo.boot.security.model.UserPrincipal;

public final class UserContext {
    private static final ThreadLocal<UserPrincipal> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(UserPrincipal userPrincipal) {
        HOLDER.set(userPrincipal);
    }

    public static UserPrincipal get() {
        return HOLDER.get();
    }

    public static Object getAttribute(String key) {
        UserPrincipal userPrincipal = get();
        return userPrincipal == null ? null : userPrincipal.getAttribute(key);
    }

    public static <T> T getAttribute(String key, Class<T> type) {
        UserPrincipal userPrincipal = get();
        return userPrincipal == null ? null : SecurityAttributes.convert(userPrincipal.getAttribute(key), type);
    }

    public static void clear() {
        HOLDER.remove();
    }

}
