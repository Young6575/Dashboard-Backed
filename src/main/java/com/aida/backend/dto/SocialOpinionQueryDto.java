package com.aida.backend.dto;

// 쿼리 2(대표의견)의 결과를 받을 임시 DTO
public record SocialOpinionQueryDto(
    String lawArticleName,
    String cat3Name,
    String sentiment,
    String content
) {}