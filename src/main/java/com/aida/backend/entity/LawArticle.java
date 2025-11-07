package com.aida.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "law_article")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class LawArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String articleNumber; // 조 번호 (예: "제1조")

    @Column(length = 500)
    private String articleTitle; // 조 제목 (예: "(목적)")

    @Column(columnDefinition = "TEXT")
    private String articleContent; // 조 내용

    // --- 부모 계층과의 관계 ---

    // '조'는 '장'에 반드시 속한다 (N:1)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id", nullable = false)
    private LawPart lawPart;

    // '조'는 '절'에 속할 수도 있다 (N:1)
    // '절'이 없는 '장'의 경우 이 값은 null이 됩니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = true) 
    private LawSection lawSection;

    // --- Category3와의 관계 (News와 동일한 전략) ---

    // 1. 외래 키(subid) 값을 직접 관리하기 위한 필드
    @Column(name = "subid")
    private String subid;

    // 2. 'subid'를 기준으로 Category3 객체를 조회(읽기)하기 위한 필드
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "subid",
        referencedColumnName = "code",
        insertable = false,
        updatable = false
    )
    private Category3 category3;
}