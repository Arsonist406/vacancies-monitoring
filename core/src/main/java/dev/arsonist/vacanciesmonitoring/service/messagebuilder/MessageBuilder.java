package dev.arsonist.vacanciesmonitoring.service.messagebuilder;

public interface MessageBuilder<T> {
    String build(T input);

    default String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
