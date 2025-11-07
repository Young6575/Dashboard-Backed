package com.aida.backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.aida.backend.dto.LegalTop5ResponseDto; // ⭐️ 1. DTO 임포트
import com.aida.backend.repository.NewsRepository;
import com.aida.backend.repository.SocialRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScoreService {

    private final NewsRepository newsRepository;
    private final SocialRepository socialRepository;

    // SCORE 모델 가중치
    private static final double W_V = 0.2;
    private static final double W_P = 0.1;
    private static final double W_G = 0.15;
    private static final double W_A = 0.1;
    private static final double W_C = 0.25;
    private static final double W_B = 0.2;

    public Map<String, LegalTop5ResponseDto> getLegalTop5(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // 1. 법안별 SCORE 지표 계산
        Map<String, LawScores> lawScores = calculateLawScores(startDateTime, endDateTime, startDate, endDate);

        // 2. Min-Max 정규화
        normalizeLawScores(lawScores);

        // 3. IIS 계산
        Map<String, Double> lawIISScores = new HashMap<>();
        for (Map.Entry<String, LawScores> entry : lawScores.entrySet()) {
            LawScores scores = entry.getValue();
            double iis = (W_V * scores.vNorm) + (W_P * scores.pNorm) + 
                        (W_G * scores.gNorm) + (W_A * scores.aNorm) +
                        (W_C * scores.cNorm) + (W_B * scores.bNorm);
            lawIISScores.put(entry.getKey(), iis);
            scores.iis = iis;
        }

        // 4. 법안별 댓글 수 조회
        Map<String, Integer> lawCommentCounts = getLawCommentCounts(startDateTime, endDateTime);

        // 5. 법안별 스탠스 집계
        Map<String, Map<String, Integer>> lawStances = getLawStances(startDateTime, endDateTime);

        // 6. TOP 5 선정 및 결과 생성
        List<Map.Entry<String, Double>> top5Laws = lawIISScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(5)
            .collect(Collectors.toList());

        Map<String, LegalTop5ResponseDto> result = new LinkedHashMap<>();
        int rank = 1;

        for (Map.Entry<String, Double> entry : top5Laws) {
            String lawName = entry.getKey();
            LawScores scores = lawScores.get(lawName);

            Map<String, Integer> stances = lawStances.getOrDefault(lawName, new HashMap<>());
            Integer commentCount = lawCommentCounts.getOrDefault(lawName, 0);

            // HOT 판정
            String hot = determineHot(scores, lawScores, lawIISScores);

            LegalTop5ResponseDto dto = new LegalTop5ResponseDto(
                lawName,
                stances.getOrDefault("개정강화", 0),
                stances.getOrDefault("폐지완화", 0),
                stances.getOrDefault("현상유지", 0),
                commentCount,
                hot
            );

            result.put(String.valueOf(rank++), dto);
        }

        return result;
    }

    /**
     * 법안별 SCORE 6가지 지표 계산
     */
    private Map<String, LawScores> calculateLawScores(
        LocalDateTime startDateTime, 
        LocalDateTime endDateTime,
        LocalDate startDate,
        LocalDate endDate
    ) {
        Map<String, LawScores> lawScores = new HashMap<>();

        // V, P, G, A 계산 (뉴스 데이터 기반)
        calculateMediaScores(lawScores, endDateTime, startDate, endDate);

        // C, B 계산 (소셜 데이터 기반)
        calculateSocialScores(lawScores, startDateTime, endDateTime);

        return lawScores;
    }

    /**
     * V, P, G, A 계산 (미디어 지표)
     */
    private void calculateMediaScores(
        Map<String, LawScores> lawScores,
        LocalDateTime endDateTime,
        LocalDate startDate,
        LocalDate endDate
    ) {
        // === V 계산: 기준일 이전 전체 데이터 ===
        List<Object[]> vData = newsRepository.findLawNewsDataForV(endDateTime);
        Map<String, Map<LocalDate, Double>> lawDailyV = new HashMap<>();

        for (Object[] row : vData) {
            String lawName = (String) row[0];
            LocalDate date = ((java.sql.Date) row[1]).toLocalDate();
            long count = ((Number) row[2]).longValue();

            long daysAgo = ChronoUnit.DAYS.between(date, endDate);
            double weight = Math.exp(-Math.log(2) * daysAgo / 7.0);
            double vScore = weight * count;

            lawDailyV.computeIfAbsent(lawName, k -> new HashMap<>())
                .merge(date, vScore, Double::sum);

            LawScores scores = lawScores.computeIfAbsent(lawName, k -> new LawScores());
            scores.vRaw += vScore;
        }

        // === P 계산: 선택 기간 내 언급 일수 비율 ===
        List<Object[]> pData = newsRepository.findLawMentionDays(
            startDate.atStartOfDay(), 
            endDate.atTime(LocalTime.MAX)
        );
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        for (Object[] row : pData) {
            String lawName = (String) row[0];
            int mentionedDays = ((Number) row[1]).intValue();

            LawScores scores = lawScores.computeIfAbsent(lawName, k -> new LawScores());
            scores.pRaw = (double) mentionedDays / totalDays;
        }

        // === G, A 계산: 주차별 성장률 및 가속도 ===
        calculateGrowthAndAcceleration(lawScores, lawDailyV, endDate);
    }

    /**
     * G, A 계산 (성장률 및 가속도)
     */
    private void calculateGrowthAndAcceleration(
        Map<String, LawScores> lawScores,
        Map<String, Map<LocalDate, Double>> lawDailyV,
        LocalDate endDate
    ) {
        for (Map.Entry<String, Map<LocalDate, Double>> entry : lawDailyV.entrySet()) {
            String lawName = entry.getKey();
            Map<LocalDate, Double> dailyV = entry.getValue();

            // 최근 3주간의 주별 V 점수 계산
            double vWeek0 = 0, vWeek1 = 0, vWeek2 = 0;

            for (Map.Entry<LocalDate, Double> dateEntry : dailyV.entrySet()) {
                LocalDate date = dateEntry.getKey();
                long weeksAgo = ChronoUnit.WEEKS.between(date, endDate);

                if (weeksAgo == 0) vWeek0 += dateEntry.getValue();
                else if (weeksAgo == 1) vWeek1 += dateEntry.getValue();
                else if (weeksAgo == 2) vWeek2 += dateEntry.getValue();
            }

            LawScores scores = lawScores.get(lawName);
            if (scores == null) continue;

            // G 계산
            if (vWeek1 > 0) {
                scores.gRaw = (vWeek0 - vWeek1) / vWeek1;
            } else {
                scores.gRaw = vWeek0;
            }

            // A 계산
            double gPrevious;
            if (vWeek2 > 0) {
                gPrevious = (vWeek1 - vWeek2) / vWeek2;
            } else {
                gPrevious = vWeek1;
            }
            scores.aRaw = scores.gRaw - gPrevious;
        }
    }

    /**
     * C, B 계산 (대중 반응 지표)
     */
    private void calculateSocialScores(
        Map<String, LawScores> lawScores,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime
    ) {
        List<Object[]> sentimentData = socialRepository.findLawSentimentData(startDateTime, endDateTime);

        Map<String, Map<String, Integer>> lawSentiments = new HashMap<>();

        for (Object[] row : sentimentData) {
            String lawName = (String) row[0];
            String sentiment = (String) row[1];
            int count = ((Number) row[2]).intValue();

            lawSentiments.computeIfAbsent(lawName, k -> new HashMap<>())
                .put(sentiment, count);
        }

        for (Map.Entry<String, Map<String, Integer>> entry : lawSentiments.entrySet()) {
            String lawName = entry.getKey();
            Map<String, Integer> sentiments = entry.getValue();

            LawScores scores = lawScores.computeIfAbsent(lawName, k -> new LawScores());

            // 찬성/반대 카운트
            int posCount = sentiments.getOrDefault("찬성_개정강화", 0) + 
                          sentiments.getOrDefault("찬성_폐지완화", 0) +
                          sentiments.getOrDefault("찬성_현상유지", 0);
            int negCount = sentiments.getOrDefault("반대_현상유지", 0) +
                          sentiments.getOrDefault("반대_폐지완화", 0) +
                          sentiments.getOrDefault("반대_개정강화", 0);
            int neuCount = sentiments.getOrDefault("중립", 0);

            // C 계산: 논쟁도
            int totalPolarized = posCount + negCount;
            if (totalPolarized > 1) {
                double balanceFactor = 1.0 - Math.abs(posCount - negCount) / (double) totalPolarized;
                scores.cRaw = totalPolarized * balanceFactor;
            } else {
                scores.cRaw = 0.0;
            }

            // B 계산: 화제성
            scores.bRaw = (double) (posCount + negCount + neuCount);
        }
    }

    /**
     * Min-Max 정규화
     */
    private void normalizeLawScores(Map<String, LawScores> lawScores) {
        if (lawScores.isEmpty()) return;

        double minV = lawScores.values().stream().mapToDouble(s -> s.vRaw).min().orElse(0.0);
        double maxV = lawScores.values().stream().mapToDouble(s -> s.vRaw).max().orElse(1.0);
        double minP = lawScores.values().stream().mapToDouble(s -> s.pRaw).min().orElse(0.0);
        double maxP = lawScores.values().stream().mapToDouble(s -> s.pRaw).max().orElse(1.0);
        double minG = lawScores.values().stream().mapToDouble(s -> s.gRaw).min().orElse(0.0);
        double maxG = lawScores.values().stream().mapToDouble(s -> s.gRaw).max().orElse(1.0);
        double minA = lawScores.values().stream().mapToDouble(s -> s.aRaw).min().orElse(0.0);
        double maxA = lawScores.values().stream().mapToDouble(s -> s.aRaw).max().orElse(1.0);
        double minC = lawScores.values().stream().mapToDouble(s -> s.cRaw).min().orElse(0.0);
        double maxC = lawScores.values().stream().mapToDouble(s -> s.cRaw).max().orElse(1.0);
        double minB = lawScores.values().stream().mapToDouble(s -> s.bRaw).min().orElse(0.0);
        double maxB = lawScores.values().stream().mapToDouble(s -> s.bRaw).max().orElse(1.0);

        for (LawScores scores : lawScores.values()) {
            scores.vNorm = normalize(scores.vRaw, minV, maxV);
            scores.pNorm = normalize(scores.pRaw, minP, maxP);
            scores.gNorm = normalize(scores.gRaw, minG, maxG);
            scores.aNorm = normalize(scores.aRaw, minA, maxA);
            scores.cNorm = normalize(scores.cRaw, minC, maxC);
            scores.bNorm = normalize(scores.bRaw, minB, maxB);
        }
    }

    private double normalize(double value, double min, double max) {
        if (max == min) return 0.0;
        return (value - min) / (max - min);
    }

    /**
     * 법안별 댓글 수 조회
     */
    private Map<String, Integer> getLawCommentCounts(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<Object[]> counts = socialRepository.findLawCommentCounts(startDateTime, endDateTime);
        return counts.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> ((Number) row[1]).intValue()
            ));
    }

    /**
     * 법안별 스탠스 집계
     */
    private Map<String, Map<String, Integer>> getLawStances(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<Object[]> stances = socialRepository.findLawStanceCounts(startDateTime, endDateTime);

        Map<String, Map<String, Integer>> result = new HashMap<>();

        for (Object[] row : stances) {
            String lawName = (String) row[0];
            String sentiment = (String) row[1];
            int count = ((Number) row[2]).intValue();

            String mappedStance = mapStance(sentiment);

            result.computeIfAbsent(lawName, k -> new HashMap<>())
                .merge(mappedStance, count, Integer::sum);
        }

        return result;
    }

    /**
     * sentiment 매핑
     */
    private String mapStance(String sentiment) {
        if (sentiment == null) return "현상유지";

        switch (sentiment) {
            case "찬성_개정강화":
                return "개정강화";
            case "찬성_폐지완화":
                return "폐지완화";
            case "반대_현상유지":
            case "반대_폐지완화":
            case "찬성_현상유지":
            case "반대_개정강화":
                return "현상유지";
            default:
                String[] parts = sentiment.split("_");
                if (parts.length >= 2) return parts[1];
                return "현상유지";
        }
    }

    /**
     * HOT 판정
     * 기준:
     * 1. IIS 상위 30% 또는
     * 2. G(성장률) 정규화 점수 > 0.7 (급성장) 또는
     * 3. C(논쟁도) 상위 20% (극심한 논쟁)
     */
    private String determineHot(
        LawScores scores, 
        Map<String, LawScores> allLawScores,
        Map<String, Double> allIISScores
    ) {
        if (allIISScores.isEmpty()) return "n";

        // 조건 1: IIS 상위 30%
        List<Double> sortedIIS = allIISScores.values().stream()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());
        int top30Index = (int) Math.ceil(sortedIIS.size() * 0.3);
        double top30Threshold = sortedIIS.get(Math.min(top30Index - 1, sortedIIS.size() - 1));
        boolean isTopIIS = scores.iis >= top30Threshold;

        // 조건 2: 성장률 높음 (정규화 점수 0.7 이상)
        boolean isHighGrowth = scores.gNorm > 0.7;

        // 조건 3: 논쟁도 상위 20%
        List<Double> sortedC = allLawScores.values().stream()
            .map(s -> s.cNorm)
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());
        int top20Index = (int) Math.ceil(sortedC.size() * 0.2);
        double top20CThreshold = sortedC.get(Math.min(top20Index - 1, sortedC.size() - 1));
        boolean isHighControversy = scores.cNorm >= top20CThreshold;

        return (isTopIIS || isHighGrowth || isHighControversy) ? "y" : "n";
    }

    /**
     * 법안별 점수를 담는 내부 클래스
     */
    private static class LawScores {
        double vRaw = 0.0, pRaw = 0.0, gRaw = 0.0, aRaw = 0.0, cRaw = 0.0, bRaw = 0.0;
        double vNorm = 0.0, pNorm = 0.0, gNorm = 0.0, aNorm = 0.0, cNorm = 0.0, bNorm = 0.0;
        double iis = 0.0;
    }
}