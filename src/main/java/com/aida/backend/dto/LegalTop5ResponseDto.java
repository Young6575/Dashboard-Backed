package com.aida.backend.dto;

public record LegalTop5ResponseDto(
    String law,
    Integer 개정강화,
    Integer 폐지완화,
    Integer 현상유지,
    Integer commentCount,
    String hot
) {}