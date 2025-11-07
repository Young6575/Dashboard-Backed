package com.aida.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HeatmapLawDto(
    String name,
    
    @JsonProperty("개정강화") // JSON 키는 "개정강화"
    int gaejeong,      // Java 변수명은 "gaejeong"
    
    @JsonProperty("폐지완화")
    int pyeji,
    
    @JsonProperty("현상유지")
    int hyunsang
) {}