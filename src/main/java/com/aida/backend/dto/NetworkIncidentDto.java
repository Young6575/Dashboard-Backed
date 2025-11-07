package com.aida.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// "name": "이슈명...", "개정강화": {...}
public record NetworkIncidentDto(
    String name,
    
    @JsonProperty("개정강화")
    NetworkStanceDetailDto gaejeong,
    
    @JsonProperty("폐지완화")
    NetworkStanceDetailDto pyeji,
    
    @JsonProperty("현상유지")
    NetworkStanceDetailDto hyunsang
) {}