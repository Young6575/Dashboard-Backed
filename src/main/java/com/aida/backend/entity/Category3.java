package com.aida.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
@NoArgsConstructor @AllArgsConstructor
@Entity @Builder
@Table(name = "category3") // 소분류
public class Category3 {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
    @ManyToOne
    @JoinColumn(name = "category2_id") // DB에 생성될 FK 컬럼 이름
    @ToString.Exclude // 순환 참조 방지
    private Category2 category2;
	
	// 소분류코드 C01001
	@Column(unique = true, nullable = false)
	private String code;
	
	// 소분류이름
    @Column(unique = true, nullable = false)
    private String name;
    
	
    // 대표기사코드
    @OneToOne
    @JoinColumn(name = "representative_news_id")
    @ToString.Exclude // 순환 참조 방지
    private News representativeNews;
    
	
	// ---------------------------------
	// ⭐️ 추가된 부분
	// ---------------------------------
    @ManyToOne
    @JoinColumn(name = "law_article_id")
    @ToString.Exclude // 순환 참조 방지
    private LawArticle lawArticle;
	
}