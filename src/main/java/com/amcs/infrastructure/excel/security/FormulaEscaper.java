package com.amcs.infrastructure.excel.security;

import java.util.Set;

/**
 * Utility protecting spreadsheet generation against CSV/Formula Injection (CWE-1236).
 *
 * <p>Any user-controlled cell string starting with dangerous command execution triggers
 * ('=', '+', '-', '@', '\t', '\r') is escaped with a leading single quote ('\'') to force
 * spreadsheet calculation engines (Excel, Calc, Sheets) to treat the cell as inert text.
 */
public final class FormulaEscaper {

    private static final Set<Character> DANGEROUS_PREFIXES = Set.of('=', '+', '-', '@', '\t', '\r');

    private FormulaEscaper() {}

    /**
     * Escapes a string if it begins with an executable formula prefix.
     *
     * @param value candidate string to write to a spreadsheet cell
     * @return escaped string prefixed with '\'' if dangerous, otherwise original string
     */
    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        char firstChar = value.charAt(0);
        if (DANGEROUS_PREFIXES.contains(firstChar)) {
            return "'" + value;
        }

        return value;
    }
}
