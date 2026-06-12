package org.gipsybuho.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class TypedValueFormatter {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<DateTimeFormatter> INPUT_DATES = List.of(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy")
    );

    private TypedValueFormatter() {}

    public static String normalizeForStorage(String dataType, String value) {
        return tryNormalizeForStorage(dataType, value)
            .orElse(value == null ? "" : value.trim());
    }

    public static Optional<String> tryNormalizeForStorage(String dataType, String value) {
        String v = value == null ? "" : value.trim();
        if (v.isBlank()) return Optional.of("");
        return switch (safeType(dataType)) {
            case "PRECIO" -> parseDecimal(v)
                .map(n -> n.setScale(2, RoundingMode.HALF_UP).toPlainString())
                .or(Optional::empty);
            case "NUMERICO" -> parseDecimal(v)
                .map(TypedValueFormatter::plainDecimal)
                .or(Optional::empty);
            case "FECHA" -> parseDate(v)
                .map(LocalDate::toString)
                .or(Optional::empty);
            default -> Optional.of(v);
        };
    }

    public static String formatForDisplay(String dataType, String value) {
        String v = value == null ? "" : value.trim();
        if (v.isBlank()) return "";
        return switch (safeType(dataType)) {
            case "PRECIO" -> parseDecimal(v)
                .map(n -> String.format(Locale.getDefault(), "%.2f €", n.doubleValue()))
                .orElse(v);
            case "NUMERICO" -> parseDecimal(v)
                .map(TypedValueFormatter::plainDecimal)
                .orElse(v);
            case "FECHA" -> parseDate(v)
                .map(d -> d.format(DISPLAY_DATE))
                .orElse(v);
            default -> v;
        };
    }

    public static Optional<BigDecimal> parseDecimal(String raw) {
        if (raw == null) return Optional.empty();
        String cleaned = raw.trim()
            .replace('\u00A0', ' ')
            .replaceAll("[^0-9,\\.\\-]", "");
        if (cleaned.isBlank() || "-".equals(cleaned)) return Optional.empty();

        boolean negative = cleaned.startsWith("-");
        cleaned = cleaned.replace("-", "");
        int lastComma = cleaned.lastIndexOf(',');
        int lastDot = cleaned.lastIndexOf('.');
        char decimalSep = 0;
        char groupingSep = 0;

        if (lastComma >= 0 && lastDot >= 0) {
            decimalSep = lastComma > lastDot ? ',' : '.';
            groupingSep = decimalSep == ',' ? '.' : ',';
        } else if (lastComma >= 0 || lastDot >= 0) {
            char sep = lastComma >= 0 ? ',' : '.';
            int pos = Math.max(lastComma, lastDot);
            int digitsAfter = cleaned.length() - pos - 1;
            int digitsBefore = pos;
            if (digitsAfter == 3 && digitsBefore > 0) {
                groupingSep = sep;
            } else {
                decimalSep = sep;
            }
        }

        if (groupingSep != 0) cleaned = cleaned.replace(String.valueOf(groupingSep), "");
        if (decimalSep != 0) cleaned = cleaned.replace(decimalSep, '.');
        if (negative) cleaned = "-" + cleaned;

        try {
            return Optional.of(new BigDecimal(cleaned));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static Optional<LocalDate> parseDate(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String v = raw.trim();
        for (DateTimeFormatter formatter : INPUT_DATES) {
            try {
                return Optional.of(LocalDate.parse(v, formatter));
            } catch (Exception ignored) {
                // Try the next accepted user-facing format.
            }
        }
        return Optional.empty();
    }

    private static String plainDecimal(BigDecimal n) {
        return n.stripTrailingZeros().toPlainString();
    }

    private static String safeType(String dataType) {
        return dataType != null ? dataType : "TEXTO";
    }
}
