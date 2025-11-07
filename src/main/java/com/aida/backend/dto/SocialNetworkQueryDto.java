package com.aida.backend.dto;

// 쿼리 1(집계)의 결과를 받을 임시 DTO
public record SocialNetworkQueryDto(
    String lawArticleName, // CONCAT(...) 결과
    String articleContent, // la.articleContent 결과
    String cat3Name,       // c3.name 결과 (incident name)
    String sentiment,
    Long count             // ⭐️ COUNT(s)는 Long 타입입니다.
) {}