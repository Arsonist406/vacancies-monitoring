package dev.arsonist.vacanciesmonitoring.dto;

public record ParserError(
        Integer code,
        String snapshotId,
        String message,
        String parserVersion,
        String timestamp
) {
}
