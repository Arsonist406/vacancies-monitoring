package dev.arsonist.vacanciesmonitoring.dto;

public record FastApiError(
        Integer code,
        String snapshotId,
        String message,
        String parserVersion,
        String timestamp
) {
}
