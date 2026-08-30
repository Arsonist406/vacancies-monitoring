package dev.arsonist.vacanciesmonitoring.exception;

public class CoreException extends RuntimeException {
    public CoreException(String message) {
        super(message);
    }

    public CoreException(Throwable cause) {
        super(cause);
    }

    public CoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
