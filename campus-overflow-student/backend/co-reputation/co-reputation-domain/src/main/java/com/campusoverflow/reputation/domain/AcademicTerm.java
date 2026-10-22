package com.campusoverflow.reputation.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;

/**
 * 排行榜周期键。学期划分（可按学校校历调整）：2–7 月为春季学期，8–12 月与次年 1 月为秋季学期。
 */
public final class AcademicTerm {

    public static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Shanghai");

    private AcademicTerm() {
    }

    public static String termKey(Instant at) {
        LocalDate date = at.atZone(CAMPUS_ZONE).toLocalDate();
        int month = date.getMonthValue();
        int year = date.getYear();
        if (month >= 2 && month <= 7) {
            return year + "-SPRING";
        }
        // 8–12 月为当年秋季；次年 1 月归属上一学年的秋季学期
        int fallYear = month == 1 ? year - 1 : year;
        return fallYear + "-FALL";
    }

    public static String weekKey(Instant at) {
        LocalDate date = at.atZone(CAMPUS_ZONE).toLocalDate();
        int weekYear = date.get(WeekFields.ISO.weekBasedYear());
        int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        return weekYear + "-W" + week;
    }

    public static String monthKey(Instant at) {
        LocalDate date = at.atZone(CAMPUS_ZONE).toLocalDate();
        return "%04d-%02d".formatted(date.getYear(), date.getMonthValue());
    }
}
