package com.aida.backend.dto;

public record DailyStanceCountDto(
    Object date,      // java.sql.Date를 받기 위해 Object 사용
    String stance,
    Long count
) {
    public java.time.LocalDate getLocalDate() {
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        } else if (date instanceof java.time.LocalDate) {
            return (java.time.LocalDate) date;
        }
        throw new IllegalStateException("Unexpected date type: " + date.getClass());
    }
}