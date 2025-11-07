package com.aida.backend.entity;


import java.time.LocalDateTime;

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

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name= "social")
public class Social {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	
	@ManyToOne
	@JoinColumn(name = "category3_id")
	private Category3 category3;
	
	
	private LocalDateTime date;
	
	@Column(columnDefinition = "TEXT")
    private String title;
	
	@Column(columnDefinition = "TEXT")
    private String content;
	
	@Column(length = 1000)
    private String url;
	
	@Column(length = 500)
    private String source;
	
	@Column(length = 500)
    private String sentiment;
	
	
	
	
	
}
