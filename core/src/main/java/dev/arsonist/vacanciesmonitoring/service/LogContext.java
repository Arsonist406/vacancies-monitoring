package dev.arsonist.vacanciesmonitoring.service;

public class LogContext {

    private static final ThreadLocal<String> LOG = new InheritableThreadLocal<>();

    private LogContext() {
    }

    public static String getLogId() {
        return LOG.get();
    }

    public static void withLogId(String logId, Runnable runnable) {
        try {
            LOG.set(logId);
            runnable.run();
        } finally {
            LOG.remove();
        }
    }
}

