package com.example.education.common.util;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    private static final DateTimeFormatter STANDARD_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeUtils() {
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(Clock.systemUTC());
    }

    public static String toIsoUtc(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.toInstant(ZoneOffset.UTC).toString();
    }

    public static String toDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(STANDARD_DATE_TIME);
    }
}

