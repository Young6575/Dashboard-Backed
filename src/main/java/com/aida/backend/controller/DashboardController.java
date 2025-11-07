package com.aida.backend.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aida.backend.dto.HeatmapResponse;
import com.aida.backend.dto.KpiJson;
import com.aida.backend.dto.LegalTop5ResponseDto;
import com.aida.backend.dto.NetworkGraphResponse;
// ⭐️ 1. 사용하지 않는 Repository 임포트 제거
// import com.aida.backend.repository.NewsRepository;
// import com.aida.backend.repository.SocialRepository;
import com.aida.backend.service.DashboardService;
import com.aida.backend.service.ScoreService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

	// ⭐️ 2. 불필요한 Repository 필드 2줄 삭제
	// private final NewsRepository newsRepository;
	// private final SocialRepository socilRepository;
	
    // ⭐️ 3. Service 필드만 남김
	private final DashboardService dashboardService;
	
	
	
	@GetMapping("/kpi-summary")
    public Map<String, KpiJson> getKpiSummary(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        return dashboardService.getKpiSummary(startDate, endDate);
    }
	
	@GetMapping("/stance-area")
	public Map<String, Object> getStanceArea(
	        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
	        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
	    
	    return dashboardService.getStanceArea(startDate, endDate);
	}
	
	@GetMapping("/social-bar")
	public Map<String, Object> getSocialBar(
	        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
	        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
	    
	    return dashboardService.getSocialBar(startDate, endDate);
	}
	
	private final ScoreService scoreService;  // 필드 추가

	@GetMapping("/legal-top5")
	public Map<String, LegalTop5ResponseDto> getLegalTop5(
	        @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
	        @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
	    
	    return scoreService.getLegalTop5(startDate, endDate);
	}
	
	/**
     * ⭐️ [신규] 히트맵 API 엔드포인트
     */
	@GetMapping("/heatmap")
    public HeatmapResponse getHeatmap(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        return dashboardService.getHeatmapData(startDate, endDate);
    }
	
	// ⭐️ [신규] 네트워크 그래프 API 엔드포인트
		@GetMapping("/network-graph")
	    public NetworkGraphResponse getNetworkGraph(
	            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
	            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
	        
	        return dashboardService.getNetworkGraph(startDate, endDate);
	    }
	
	
	
}