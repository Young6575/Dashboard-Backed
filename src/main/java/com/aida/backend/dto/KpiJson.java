package com.aida.backend.dto;

import java.util.List;

public record KpiJson(KpiSummary summary, List<KpiDailyData> dailyData) {}
