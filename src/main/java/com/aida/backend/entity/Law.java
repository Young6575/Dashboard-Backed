package com.aida.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "law", uniqueConstraints = {
    @UniqueConstraint(columnNames = "lawName") // 법률명은 고유해야 함
})
@Getter @Setter
public class Law {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String lawName; // 법률명 (예: "개인정보보호법")

    // lawCode, url 등 법률 자체의 추가 정보 필드...
}