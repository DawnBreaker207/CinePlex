package com.dawn.ai.dto;

public record InsightResponse(
        String type,
        String severity,
        String title,
        String detail) {
}