package com.aida.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StanceAreaDto(
    String date,
    @JsonProperty("개정강화") Integer revision,
    @JsonProperty("폐지완화") Integer abolition,
    @JsonProperty("현상유지") Integer maintain
) {}