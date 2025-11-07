package com.aida.backend.dto;

import java.time.LocalDate;

public record DailyCountDto(
    String category,
    Object date,      // ⭐️ LocalDate → Object로 변경
    Long count
) {
    // ⭐️ 편의 메서드 추가: Object를 LocalDate로 변환
    public LocalDate getLocalDate() {
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        } else if (date instanceof LocalDate) {
            return (LocalDate) date;
        }
        throw new IllegalStateException("Unexpected date type: " + date.getClass());
    }
}