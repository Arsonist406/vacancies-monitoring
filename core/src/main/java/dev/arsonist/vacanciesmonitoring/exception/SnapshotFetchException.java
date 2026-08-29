package dev.arsonist.vacanciesmonitoring.exception;

public class SnapshotFetchException extends RuntimeException {
    public SnapshotFetchException(Throwable cause) {
        super(cause);
    }

    public SnapshotFetchException(String message) {
        super(message);
    }
}
