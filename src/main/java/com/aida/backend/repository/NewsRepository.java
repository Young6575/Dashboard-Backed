package com.aida.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aida.backend.dto.CategoryArticleCountDto;
import com.aida.backend.dto.DailyCountDto;
import com.aida.backend.entity.News;

public interface NewsRepository extends JpaRepository<News, Long> {

	// 대분류별 기간 개수
    @Query("SELECT n.category AS category, COUNT(n) AS count " +
           "FROM News n " +
           "WHERE n.date BETWEEN :startDate AND :endDate " +
           "GROUP BY n.category")
    List<CategoryArticleCountDto> countByDateBetweenGroupByCategories(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    
    /**
     * (일별 데이터용) (수정 X)
     */
    @Query("SELECT new com.aida.backend.dto.DailyCountDto(n.category, FUNCTION('DATE', n.date), COUNT(n)) " +
           "FROM News n " +
           "WHERE n.date BETWEEN :startDate AND :endDate " +
           "GROUP BY n.category, FUNCTION('DATE', n.date)")
    List<DailyCountDto> findDailyCountsGroupBy(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * ⭐️ [수정] 법안별(조항/제목 포함) 뉴스 데이터 조회 (V, P, G, A 계산용)
     */
    @Query("SELECT CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, '')) as lawName, " +
           "FUNCTION('DATE', n.date) as date, COUNT(n) as count " +
           "FROM News n " +
           "JOIN n.category3 c3 " +
           "JOIN c3.lawArticle la " +
           "JOIN la.lawPart lp " +
           "JOIN lp.law law " +
           "WHERE n.date <= :endDate " +
           "GROUP BY lawName, FUNCTION('DATE', n.date)")
    List<Object[]> findLawNewsDataForV(@Param("endDate") LocalDateTime endDate);

    /**
     * ⭐️ [수정] 선택 기간 내 법안별(조항/제목 포함) 언급된 날짜 수 (P 계산용)
     */
    @Query("SELECT CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, '')), " +
           "COUNT(DISTINCT FUNCTION('DATE', n.date)) " +
           "FROM News n " +
           "JOIN n.category3 c3 " +
           "JOIN c3.lawArticle la " +
           "JOIN la.lawPart lp " +
           "JOIN lp.law law " +
           "WHERE n.date BETWEEN :startDate AND :endDate " +
           "GROUP BY CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, ''))")
    List<Object[]> findLawMentionDays(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * ⭐️ [수정] 법안별(조항/제목 포함) 주차별 V 스코어 계산용 (G, A 계산)
     */
    @Query("SELECT CONCAT(law.lawName, ' ', la.articleNumber, COALESCE(la.articleTitle, '')) as lawName, " +
           "FUNCTION('DATE', n.date) as date, COUNT(n) as count " +
           "FROM News n " +
           "JOIN n.category3 c3 " +
           "JOIN c3.lawArticle la " +
           "JOIN la.lawPart lp " +
           "JOIN lp.law law " +
           "WHERE n.date BETWEEN :startDate AND :endDate " +
           "GROUP BY lawName, FUNCTION('DATE', n.date)")
    List<Object[]> findLawNewsDataForWeeks(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    
	
}