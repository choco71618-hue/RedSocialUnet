package com.redsocial.ui.util;

import com.redsocial.common.dto.UserDto;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Span;

public final class AvatarUtil {

    private static final int[] PALETTE = {
            0x5b8dee, 0x8b5cf6, 0x06b6d4, 0x10b981, 0xf59e0b, 0xef4444,
            0xec4899, 0x6366f1, 0x14b8a6, 0xf97316
    };

    private AvatarUtil() {
    }

    public static Avatar of(UserDto user) {
        return create(
                user == null ? null : user.displayName(),
                user == null ? null : user.username(),
                user == null ? null : user.avatarUrl());
    }

    public static Avatar create(String displayName, String username, String avatarUrl) {
        String name = displayName;
        if (name == null || name.isBlank()) {
            name = username;
        }
        if (name == null || name.isBlank()) {
            name = "?";
        }
        Avatar avatar = new Avatar(name);
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            avatar.setImage(avatarUrl);
        } else {
            hex(avatar, name, username);
        }
        avatar.addClassName("rs-avatar");
        return avatar;
    }

    /** Genera un placeholder de inicial con gradiente determinístico (sin avatarUrl). */
    public static Span fallback(String displayName, String username) {
        String name = displayName;
        if (name == null || name.isBlank()) {
            name = username;
        }
        if (name == null || name.isBlank()) {
            name = "?";
        }
        int base = username == null ? name.hashCode() : username.hashCode();
        int index = Math.floorMod(base, PALETTE.length);
        Span span = new Span(initial(name));
        span.addClassName("avatar-fallback");
        span.addClassName("avatar-hue-" + index);
        return span;
    }

    private static String initial(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        return Character.toUpperCase(trimmed.charAt(0)) + "";
    }

    private static void hex(Avatar avatar, String name, String username) {
        int base = username == null ? name.hashCode() : username.hashCode();
        int index = Math.floorMod(base, PALETTE.length);
        avatar.addClassName("avatar-hue-" + index);
    }
}