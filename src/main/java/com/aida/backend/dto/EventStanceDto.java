package com.aida.backend.dto;

public record EventStanceDto(
    String eventName,
    String sentiment,
    Long count
) {}