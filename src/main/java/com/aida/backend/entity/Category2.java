package com.aida.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import lombok.ToString;

@Getter @Setter @ToString
@NoArgsConstructor @AllArgsConstructor
@Entity @Builder
@Table(name = "category2") // 중분류
public class Category2 {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
    @ManyToOne
    @JoinColumn(name = "category1_id") // DB에 생성될 FK 컬럼 이름
    private Category1 category1;
	
	// 중분류코드 C01
	@Column(unique = true, nullable = false)
	private String code;
	
	// 중분류이름
    @Column(unique = true, nullable = false)
    private String name;
	
	
	
}
