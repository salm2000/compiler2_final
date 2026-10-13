package edu.compiler.diagnostics;

public record Diagnostic(String file, int line, int column, String code, Severity severity, String message) {
    public enum Severity { ERROR, WARNING }
    @Override public String toString() {
        return "%s:%d:%d: %s %s: %s".formatted(file, line, column, severity, code, message);
    }
}

