package com.aida.backend.entity;

import java.time.LocalDateTime;

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
@Table(name = "news")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class News {
	
    // EAGER: FirstTable 조회 시 SecondTable도 즉시 함께 조회
    // LAZY: SecondTable이 실제로 필요할 때 조회 (권장)


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 1. 관계를 맺을 외래 키 필드
    // DB의 'subid' 컬럼과 직접 매핑됩니다.
    // 이 필드를 통해 subid 값을 저장하거나 조회할 수 있습니다.
    private Long subid; 
    
    // 2. 1번의 'subid'를 이용해 ClassfiyID 엔티티와 관계를 맺음
    // insertable=false, updatable=false:
    // 이 관계(객체)를 통해 'subid' 컬럼을 직접 수정/삽입하지 않도록 설정합니다.
    // 'subid' 값의 관리는 위 1번 필드를 통해 이루어집니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subid", referencedColumnName = "code", insertable = false, updatable = false)
    private Category3 category3;

    private String category;
    private LocalDateTime date;
    
  
    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 1000)
    private String url;
    
    
    
}
