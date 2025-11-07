package com.aida.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "category1") // 대분류
public class Category1 {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	// 대분류코드 C
	@Column(unique = true, nullable = false)
	private String code;
	
	// 대분류이름 "개인정보보호법"
    @Column(unique = true, nullable = false)
    private String name;

}
