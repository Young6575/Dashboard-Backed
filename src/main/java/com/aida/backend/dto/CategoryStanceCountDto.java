package com.aida.backend.dto;

public record CategoryStanceCountDto(
    String category,
    String sentiment,
    Long count
) {}