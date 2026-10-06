package com.redsocial.ui.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeFormat {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private TimeFormat() {
    }

    public static String relative(Instant instant) {
        if (instant == null) {
            return "";
        }
        long seconds = Duration.between(instant, Instant.now()).getSeconds();
        if (seconds < 60) {
            return "ahora";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return "hace " + minutes + " min";
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return "hace " + hours + " h";
        }
        long days = hours / 24;
        if (days < 30) {
            return "hace " + days + " d";
        }
        return DATE_TIME.format(instant);
    }
}
