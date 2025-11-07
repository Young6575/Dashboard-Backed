package com.aida.backend.dto;

public record KpiSummary(long totalArticles, int totalComments, double newsGrowthRate, double socialGrowthRate) {}
