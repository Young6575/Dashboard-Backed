package com.aida.backend.dto;

import java.time.LocalDate;

public record EventMetricsDto(
    String eventName,
    String category,
    LocalDate date,
    Double vScore,
    Double pScore,
    Double gScore,
    Double aScore,
    Double cScore,
    Double bScore
) {}