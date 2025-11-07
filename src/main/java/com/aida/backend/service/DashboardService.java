package com.aida.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.aida.backend.dto.CategoryArticleCountDto;
import com.aida.backend.dto.CategoryStanceCountDto;
import com.aida.backend.dto.DailyCountDto;
import com.aida.backend.dto.DailyStanceCountDto;
import com.aida.backend.dto.HeatmapLawDto;
import com.aida.backend.dto.HeatmapResponse;
import com.aida.backend.dto.KpiDailyData;
import com.aida.backend.dto.KpiJson;
import com.aida.backend.dto.KpiSummary;
import com.aida.backend.dto.LegalTop5ResponseDto; // ⭐️ 1. DTO 임포트
import com.aida.backend.dto.NetworkGraphResponse;
import com.aida.backend.dto.NetworkIncidentDto;
import com.aida.backend.dto.NetworkNodeDto;
import com.aida.backend.dto.NetworkStanceDetailDto;
import com.aida.backend.dto.SocialNetworkQueryDto;
import com.aida.backend.dto.SocialOpinionQueryDto;
import com.aida.backend.entity.Category1;
import com.aida.backend.repository.Category1Repository;
import com.aida.backend.repository.NewsRepository;
import com.aida.backend.repository.SocialRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final Category1Repository category1Repository;
    private final NewsRepository newsRepository;
    private final SocialRepository socialRepository;
    private final ScoreService scoreService;

    public Map<String, KpiJson> getKpiSummary(LocalDate startDate, LocalDate endDate) {

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        Map<String, KpiJson> response = new LinkedHashMap<>();

        // === 1. 전체 기간 개수 (Summary용) ===
        Map<String, Long> articleCountMap = newsRepository.countByDateBetweenGroupByCategories(startDateTime, endDateTime)
                .stream()
                .collect(Collectors.toMap(
                        CategoryArticleCountDto::category,
                        CategoryArticleCountDto::count
                ));

        Map<String, Long> commentCountMap = socialRepository.countByDateBetweenGroupByCategories(startDateTime, endDateTime)
                .stream()
                .collect(Collectors.toMap(
                        CategoryArticleCountDto::category,
                        CategoryArticleCountDto::count
                ));

        // === 2. 일별 개수 (dailyData 및 growthRate용) ===

        // ⭐️ FUNCTION('DATE', s.date)의 결과 타입은 java.sql.Date이므로 LocalDate로 변환
        Map<String, Map<LocalDate, Integer>> dailyNewsMap =
        	    newsRepository.findDailyCountsGroupBy(startDateTime, endDateTime)
        	        .stream()
        	        .collect(Collectors.groupingBy(
        	            DailyCountDto::category,
        	            Collectors.toMap(
        	                dto -> dto.getLocalDate(),  // ⭐️ date() → getLocalDate()로 변경
        	                dto -> dto.count().intValue()
        	            )
        	        ));

        	Map<String, Map<LocalDate, Integer>> dailySocialMap =
        	    socialRepository.findDailyCountsGroupBy(startDateTime, endDateTime)
        	        .stream()
        	        .collect(Collectors.groupingBy(
        	            DailyCountDto::category,
        	            Collectors.toMap(
        	                dto -> dto.getLocalDate(),  // ⭐️ date() → getLocalDate()로 변경
        	                dto -> dto.count().intValue()
        	            )
        	        ));

        // === 3. 모든 카테고리 목록 ===
        List<Category1> allCategories = category1Repository.findAll();

        // === 4. 데이터 조합 (DB 접근 X) ===
        for (Category1 cat1 : allCategories) {
            String categoryName = cat1.getName();

            Map<LocalDate, Integer> catDailyNews = dailyNewsMap.getOrDefault(categoryName, Collections.emptyMap());
            Map<LocalDate, Integer> catDailySocial = dailySocialMap.getOrDefault(categoryName, Collections.emptyMap());

            // 날짜 순 정렬을 위한 TreeSet
            Set<LocalDate> allDates = new TreeSet<>(catDailyNews.keySet());
            allDates.addAll(catDailySocial.keySet());

            // === 5. 일별 데이터 조립 ===
            List<KpiDailyData> finalDailyDataList = new ArrayList<>();
            for (LocalDate date : allDates) {
                int newsCount = catDailyNews.getOrDefault(date, 0);
                int socialCount = catDailySocial.getOrDefault(date, 0);

                // LocalDate를 String으로 변환 (예: "2025-11-03")
                finalDailyDataList.add(new KpiDailyData(date.toString(), newsCount, socialCount));
            }

            // === 6. 성장률 계산 ===
            double newsGrowthRate = calculateAverageGrowthRate(finalDailyDataList, true);
            double socialGrowthRate = calculateAverageGrowthRate(finalDailyDataList, false);

            // === 7. Summary 계산 ===
            int totalArticles = articleCountMap.getOrDefault(categoryName, 0L).intValue();
            int totalComments = commentCountMap.getOrDefault(categoryName, 0L).intValue();

            KpiSummary summary = new KpiSummary(totalArticles, totalComments, newsGrowthRate, socialGrowthRate);
            KpiJson kpiJson = new KpiJson(summary, finalDailyDataList);

            response.put(categoryName, kpiJson);
        }

        return response;
    }
    
    public Map<String, Object> getStanceArea(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // ⭐️ Social 데이터만 조회 (News는 제외)
        List<DailyStanceCountDto> socialCounts = socialRepository.findDailyStanceCounts(startDateTime, endDateTime);

        // 2. 날짜별로 그룹화 후 스탠스별로 합산
        Map<LocalDate, Map<String, Integer>> dateStanceMap = socialCounts.stream()
            .collect(Collectors.groupingBy(
                DailyStanceCountDto::getLocalDate,
                TreeMap::new,  // 날짜 순 정렬
                Collectors.groupingBy(
                    dto -> mapStance(dto.stance()),  // sentiment 값을 매핑
                    Collectors.summingInt(dto -> dto.count().intValue())
                )
            ));

        // 3. 최종 결과 리스트 생성
        List<Map<String, Object>> data = new ArrayList<>();
        
        for (Map.Entry<LocalDate, Map<String, Integer>> entry : dateStanceMap.entrySet()) {
            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", entry.getKey().toString());
            
            Map<String, Integer> stances = entry.getValue();
            dayData.put("개정강화", stances.getOrDefault("개정강화", 0));
            dayData.put("폐지완화", stances.getOrDefault("폐지완화", 0));
            dayData.put("현상유지", stances.getOrDefault("현상유지", 0));
            
            data.add(dayData);
        }

        return Map.of("data", data);
    }

    public Map<String, Object> getSocialBar(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 1. 카테고리별 스탠스 개수 조회
        List<CategoryStanceCountDto> counts = socialRepository.findCategoryStanceCounts(startDateTime, endDateTime);

        // 2. 카테고리별로 그룹화 후 스탠스별 합산
        Map<String, Map<String, Integer>> categoryStanceMap = counts.stream()
            .collect(Collectors.groupingBy(
                CategoryStanceCountDto::category,
                LinkedHashMap::new,  // 순서 유지
                Collectors.groupingBy(
                    dto -> mapStance(dto.sentiment()),
                    Collectors.summingInt(dto -> dto.count().intValue())
                )
            ));

        // 3. 최종 결과 리스트 생성
        List<Map<String, Object>> data = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, Integer>> entry : categoryStanceMap.entrySet()) {
            Map<String, Object> categoryData = new LinkedHashMap<>();
            categoryData.put("category", entry.getKey());
            
            Map<String, Integer> stances = entry.getValue();
            categoryData.put("개정강화", stances.getOrDefault("개정강화", 0));
            categoryData.put("폐지완화", stances.getOrDefault("폐지완화", 0));
            categoryData.put("현상유지", stances.getOrDefault("현상유지", 0));
            
            data.add(categoryData);
        }

        return Map.of("data", data);
    }


   
    /**
     * DB의 sentiment 값을 화면 표시용으로 매핑
     * "찬성_개정강화" → "개정강화"
     * "반대_현상유지" → "현상유지"
     * "찬성_폐지완화" → "폐지완화"
     */
    private String mapStance(String dbStance) {
        if (dbStance == null) {
            return "현상유지";
        }
        
        switch (dbStance) {
            case "찬성_개정강화":
                return "개정강화";
            case "찬성_폐지완화":
                return "폐지완화";
            case "반대_현상유지":
            case "반대_폐지완화":
            case "찬성_현상유지":
            case "반대_개정강화": // ⭐️ 이 케이스를 추가해야 합니다.
                return "현상유지";
            default:
                // "중립" 등 그 외의 모든 것은 현상유지로 처리
                return "현상유지";
        }
    }

    /**
     * 일별 성장률 평균 계산
     */
    private double calculateAverageGrowthRate(List<KpiDailyData> dailyData, boolean isNews) {
        if (dailyData.size() < 2) {
            return 0.0; // 데이터가 2일치 미만이면 성장률 계산 불가
        }

        List<Double> dailyRates = new ArrayList<>();

        for (int i = 1; i < dailyData.size(); i++) {
            KpiDailyData prevDay = dailyData.get(i - 1);
            KpiDailyData currDay = dailyData.get(i);

            int prevCount = isNews ? prevDay.news() : prevDay.social();
            int currCount = isNews ? currDay.news() : currDay.social();

            if (prevCount > 0) { // 분모가 0이 아닐 때만 계산
                double rate = (double) (currCount - prevCount) / prevCount;
                dailyRates.add(rate);
            }
        }

        if (dailyRates.isEmpty()) {
            return 0.0; // 유효한 성장률 기간이 없는 경우
        }

        // 모든 일별 성장률의 평균 반환
        return dailyRates.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }
    
    /**
     * [신규] 히트맵 데이터 API
     */
    public HeatmapResponse getHeatmapData(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 1. DB에서 (법안명, Raw센티멘트, 개수) 리스트를 가져옴
        List<CategoryStanceCountDto> dbData = socialRepository.findCategoryStanceCountsForHeatmap(startDateTime, endDateTime);

        // 2. 피벗(Pivot)을 위한 임시 Map 생성
        // Key: 법안명, Value: [Key: 매핑된 스탠스, Value: 개수]
        Map<String, Map<String, Integer>> pivotMap = new HashMap<>();

        for (CategoryStanceCountDto row : dbData) {
            String lawName = row.category();
            String rawSentiment = row.sentiment();
            int count = row.count().intValue();
            
            // 3. ScoreService의 로직을 재사용하여 6개를 3개로 매핑
            String mappedStance = mapStance(rawSentiment); // (e.g., "반대_현상유지" -> "현상유지")

            // 4. Map에 누적
            // "개인정보보호법" 맵을 찾고, 그 안의 "현상유지" 카운트를 증가시킴
            pivotMap.computeIfAbsent(lawName, k -> new HashMap<>())
                    .merge(mappedStance, count, Integer::sum);
        }

        // 5. 임시 Map을 최종 DTO 리스트로 변환
        List<HeatmapLawDto> dtoList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> entry : pivotMap.entrySet()) {
            String lawName = entry.getKey();
            Map<String, Integer> stances = entry.getValue();

            dtoList.add(new HeatmapLawDto(
                lawName,
                stances.getOrDefault("개정강화", 0), // "개정강화" 키가 없으면 0
                stances.getOrDefault("폐지완화", 0),
                stances.getOrDefault("현상유지", 0)
            ));
        }

        // 6. "laws" 키로 감싸서 반환
        return new HeatmapResponse(dtoList);
    }
    

    /**
     * ⭐️ [수정] 네트워크 그래프 API
     * (ScoreService의 Top 5 법안의 '정렬 순서'를 유지하도록 수정)
     */
    public NetworkGraphResponse getNetworkGraph(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // === 1. ScoreService를 호출해 Top 5 법안 목록을 가져옴 ===
        // ⭐️ top5Map은 'LinkedHashMap'이므로 순서가 "1" -> "2" -> ... 로 보장됨
        Map<String, LegalTop5ResponseDto> top5Map = scoreService.getLegalTop5(startDate, endDate);
        
        // ⭐️ [수정] .toSet()을 호출하지 않고, 순서가 보장된 'values()' 컬렉션을 그대로 사용
        Collection<LegalTop5ResponseDto> top5DtosInOrder = top5Map.values();

        // === 2. '집계(Count)' 및 '의견(Content)' 데이터 가져오기 (DB 접근) ===
        // (이 부분은 모든 법안 데이터를 가져오는 것이 효율적이므로 그대로 둡니다)
        List<SocialNetworkQueryDto> dbCounts = socialRepository.findNetworkGraphDataByLawArticle(startDateTime, endDateTime);
        List<SocialOpinionQueryDto> dbOpinions = socialRepository.findNetworkGraphOpinions(startDateTime, endDateTime);

        // === 3. 데이터 맵으로 변환 (전체 데이터) ===
        // (이 부분도 그대로 둡니다)
        Map<String, Map<String, Map<String, Integer>>> countMap = new HashMap<>();
        Map<String, String> descriptionMap = new HashMap<>();
        for (SocialNetworkQueryDto countResult : dbCounts) {
            countMap
                .computeIfAbsent(countResult.lawArticleName(), k -> new HashMap<>())
                .computeIfAbsent(countResult.cat3Name(), k -> new HashMap<>())
                .put(countResult.sentiment(), countResult.count().intValue());
            descriptionMap.putIfAbsent(countResult.lawArticleName(), countResult.articleContent());
        }
        Map<String, Map<String, Map<String, List<String>>>> opinionMap = new HashMap<>();
        for (SocialOpinionQueryDto opinion : dbOpinions) {
             List<String> opinions = opinionMap
                .computeIfAbsent(opinion.lawArticleName(), k -> new HashMap<>())
                .computeIfAbsent(opinion.cat3Name(), k -> new HashMap<>())
                .computeIfAbsent(opinion.sentiment(), k -> new ArrayList<>());
            if (opinions.size() < 5) {
                opinions.add(opinion.content());
            }
        }
        
        // === 4. [수정] '순서가 보장된' Top 5 목록을 기준으로 최종 DTO 조립 ===
        List<NetworkNodeDto> nodes = new ArrayList<>();

        // ⭐️ [수정] Set(top5LawNames)이 아닌, 순서가 있는 Collection(top5DtosInOrder)으로 루프
        for (LegalTop5ResponseDto top5Dto : top5DtosInOrder) {
            
            String lawArticleName = top5Dto.law(); // ⭐️ 루프 안에서 법안 이름을 순서대로 가져옴

            String description = descriptionMap.getOrDefault(lawArticleName, "설명 없음");
            Map<String, Map<String, Integer>> cat3CountMap = countMap.getOrDefault(lawArticleName, Collections.emptyMap());
            
            List<NetworkIncidentDto> incidents = new ArrayList<>();

            // 2단계 루프 (Cat3/Incident 기준)
            for (Map.Entry<String, Map<String, Integer>> cat3Entry : cat3CountMap.entrySet()) {
                String cat3Name = cat3Entry.getKey();
                Map<String, Integer> stanceCounts = cat3Entry.getValue();

                // (의견 가져오는 로직은 동일)
                List<String> gaejeongOpinions = opinionMap.getOrDefault(lawArticleName, Collections.emptyMap()).getOrDefault(cat3Name, Collections.emptyMap()).getOrDefault("찬성_개정강화", Collections.emptyList());
                List<String> pyejiOpinions = opinionMap.getOrDefault(lawArticleName, Collections.emptyMap()).getOrDefault(cat3Name, Collections.emptyMap()).getOrDefault("찬성_폐지완화", Collections.emptyList());
                List<String> hyunsangOpinions = opinionMap.getOrDefault(lawArticleName, Collections.emptyMap()).getOrDefault(cat3Name, Collections.emptyMap()).getOrDefault("반대_현상유지", Collections.emptyList());

                NetworkStanceDetailDto gaejeong = new NetworkStanceDetailDto(stanceCounts.getOrDefault("찬성_개정강화", 0), gaejeongOpinions);
                NetworkStanceDetailDto pyeji = new NetworkStanceDetailDto(stanceCounts.getOrDefault("찬성_폐지완화", 0), pyejiOpinions);
                NetworkStanceDetailDto hyunsang = new NetworkStanceDetailDto(stanceCounts.getOrDefault("반대_현상유지", 0), hyunsangOpinions);
                
                incidents.add(new NetworkIncidentDto(cat3Name, gaejeong, pyeji, hyunsang));
            }
            
            // ⭐️ nodes 리스트에 1등, 2등, 3등... 순서대로 추가됨
            nodes.add(new NetworkNodeDto(lawArticleName, description, incidents));
        }

        return new NetworkGraphResponse(nodes);
    }
    
    
}
