package com.aida.backend.dto;

import java.util.List;

// "label": "법안명...", "description": "법안내용...", "incidents": [...]
public record NetworkNodeDto(
    String label,
    String description,
    List<NetworkIncidentDto> incidents
) {}