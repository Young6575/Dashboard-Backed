package com.aida.backend.dto;

import java.util.List;

// "count": 450, "opinions": [...]
public record NetworkStanceDetailDto(
    int count,
    List<String> opinions
) {}