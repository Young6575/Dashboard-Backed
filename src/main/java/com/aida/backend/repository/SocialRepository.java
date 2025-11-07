package com.aida.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aida.backend.dto.CategoryArticleCountDto;
import com.aida.backend.dto.CategoryStanceCountDto;
import com.aida.backend.dto.DailyCountDto;
import com.aida.backend.dto.DailyStanceCountDto;
import com.aida.backend.dto.SocialNetworkQueryDto;
import com.aida.backend.dto.SocialOpinionQueryDto;
// ... (다른 DTO import)
import com.aida.backend.entity.Social;

public interface SocialRepository extends JpaRepository<Social, Long> {

    /**
     * (요약용) 카테고리별 '전체' 개수 (수정 X)
     */
    @Query("SELECT c1.name AS category, COUNT(s) AS count " +
           "FROM Social s " +
           "JOIN s.category3 c3 " +
           "JOIN c3.category2 c2 " +
           "JOIN c2.category1 c1 " +
           "WHERE s.date BETWEEN :startDate AND :endDate " +
           "GROUP BY c1.name")
    List<CategoryArticleCountDto> countByDateBetweenGroupByCategories(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
	
    /**
     * (일별 데이터용) (수정 X)
     */
    @Query("SELECT new com.aida.backend.dto.DailyCountDto(c1.name, FUNCTION('DATE', s.date), COUNT(s)) " +
            "FROM Social s " +
            "JOIN s.category3 c3 " +
            "JOIN c3.category2 c2 " +
            "JOIN c2.category1 c1 " +
            "WHERE s.date BETWEEN :startDate AND :endDate " +
            "GROUP BY c1.name, FUNCTION('DATE', s.date)")
     List<DailyCountDto> findDailyCountsGroupBy(
         @Param("startDate") LocalDateTime startDate,
         @Param("endDate") LocalDateTime endDate
     );
    
    
    /**
     * 스탠스별 일별 개수 (수정 X)
     */
    @Query("SELECT new com.aida.backend.dto.DailyStanceCountDto(" +
            "FUNCTION('DATE', s.date), s.sentiment, COUNT(s)) " +
            "FROM Social s " +
            "WHERE s.date BETWEEN :startDate AND :endDate " +
            "AND s.sentiment IN ('찬성_개정강화', '찬성_폐지완화', '반대_현상유지') " +
            "GROUP BY FUNCTION('DATE', s.date), s.sentiment")
     List<DailyStanceCountDto> findDailyStanceCounts(
         @Param("startDate") LocalDateTime startDate,
         @Param("endDate") LocalDateTime endDate
     );
    
    /**
     * 카테고리별 스탠스 개수 (수정 X)
     */
    @Query("SELECT new com.aida.backend.dto.CategoryStanceCountDto(" +
           "c1.name, s.sentiment, COUNT(s)) " +
           "FROM Social s " +
           "JOIN s.category3 c3 " +
           "JOIN c3.category2 c2 " +
           "JOIN c2.category1 c1 " +
           "WHERE s.date BETWEEN :startDate AND :endDate " +
           "AND s.sentiment IN ('찬성_개정강화', '찬성_폐지완화', '반대_현상유지') " +
           "GROUP BY c1.name, s.sentiment")
    List<CategoryStanceCountDto> findCategoryStanceCounts(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    
    /**
     * ⭐️ [수정] 법안별(조항/제목 포함) sentiment 집계 (C, B 계산용)
     */
    @Query("SELECT CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, '')), " +
           "s.sentiment, COUNT(s) " +
           "FROM Social s " +
           "JOIN s.category3 c3 " +
           "JOIN c3.lawArticle la " +
           "JOIN la.lawPart lp " +
           "JOIN lp.law law " +
           "WHERE s.date BETWEEN :startDate AND :endDate " +
           "GROUP BY CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, '')), s.sentiment")
    List<Object[]> findLawSentimentData(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * ⭐️ [수정] 법안별(조항/제목 포함) 댓글 총 개수
     */
    @Query("SELECT CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, '')), " +
           "COUNT(s) " +
           "FROM Social s " +
           "JOIN s.category3 c3 " +
           "JOIN c3.lawArticle la " +
           "JOIN la.lawPart lp " +
           "JOIN lp.law law " +
           "WHERE s.date BETWEEN :startDate AND :endDate " +
           "GROUP BY CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, ''))")
    List<Object[]> findLawCommentCounts(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * ⭐️ [수정] 법안별(조항/제목 포함) 스탠스 집계
     */
    @Query("SELECT CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, '')), " +
           "s.sentiment, COUNT(s) " +
           "FROM Social s " +
           "JOIN s.category3 c3 " +
           "JOIN c3.lawArticle la " +
           "JOIN la.lawPart lp " +
           "JOIN lp.law law " +
           "WHERE s.date BETWEEN :startDate AND :endDate " +
           "AND s.sentiment IN ('찬성_개정강화', '찬성_폐지완화', '반대_현상유지') " +
           "GROUP BY CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, '')), s.sentiment")
    List<Object[]> findLawStanceCounts(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * 카테고리별 스탠스 개수 (sentiment 기준)
     * ⭐️ [수정] 3개가 아닌 6개 sentiment를 모두 포함하도록 IN 절 수정
     */
    @Query("SELECT new com.aida.backend.dto.CategoryStanceCountDto(" +
           "c1.name, s.sentiment, COUNT(s)) " +
           "FROM Social s " +
           "JOIN s.category3 c3 " +
           "JOIN c3.category2 c2 " +
           "JOIN c2.category1 c1 " +
           "WHERE s.date BETWEEN :startDate AND :endDate " +
           "AND s.sentiment IN ('찬성_개정강화', '찬성_폐지완화', '반대_현상유지') " + 
           "GROUP BY c1.name, s.sentiment")
    // ⭐️ 1. 메서드명 변경
    List<CategoryStanceCountDto> findCategoryStanceCountsForHeatmap( 
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * ⭐️ [신규 1] 네트워크 그래프용 '집계(Count)' 데이터 조회 (LawArticle 기준)
     */
    @Query("SELECT new com.aida.backend.dto.SocialNetworkQueryDto(" +
           "CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, '')), " + // lawArticleName
           "la.articleContent, " + // articleContent
           "c3.name, " +           // cat3Name
           "s.sentiment, COUNT(s)) " +
           "FROM Social s " +
           "JOIN s.category3 c3 " +
           "JOIN c3.lawArticle la " +
           "JOIN la.lawPart lp " +
           "JOIN lp.law law " +
           "WHERE s.date BETWEEN :startDate AND :endDate " +
           "AND s.sentiment IN ('찬성_개정강화', '찬성_폐지완화', '반대_현상유지') " +
           "GROUP BY CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, '')), la.articleContent, c3.name, s.sentiment")
    List<SocialNetworkQueryDto> findNetworkGraphDataByLawArticle(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * ⭐️ [신규 2] 네트워크 그래프용 '대표 의견(content)' 조회
     */
    @Query("SELECT new com.aida.backend.dto.SocialOpinionQueryDto(" +
           "CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, '')), " + // lawArticleName
           "c3.name, " +           // cat3Name
           "s.sentiment, " +       // sentiment
           "s.content) " +         // content
           "FROM Social s " +
           "JOIN s.category3 c3 " +
           "JOIN c3.lawArticle la " +
           "JOIN la.lawPart lp " +
           "JOIN lp.law law " +
           "WHERE s.date BETWEEN :startDate AND :endDate " +
           "AND s.sentiment IN ('찬성_개정강화', '찬성_폐지완화', '반대_현상유지')")
    List<SocialOpinionQueryDto> findNetworkGraphOpinions(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    
    
    
}