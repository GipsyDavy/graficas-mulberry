package org.gipsybuho.service;

import java.util.Optional;

public record ValidationIssue(
    int rowIndex,
    String columnName,
    String issue,
    Optional<String> suggestedFix,
    Severity severity
) {
    public enum Severity { ERROR, WARNING }
}
