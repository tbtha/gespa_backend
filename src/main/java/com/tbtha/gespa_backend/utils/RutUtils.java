package com.tbtha.gespa_backend.utils;

public final class RutUtils {

    private RutUtils() {
    }

    public static String normalize(String rawRut) {
        if (rawRut == null) {
            return "";
        }

        String compact = rawRut.replaceAll("[^0-9kK]", "").toUpperCase();
        if (compact.length() < 2) {
            return compact;
        }

        String body = compact.substring(0, compact.length() - 1);
        String dv = compact.substring(compact.length() - 1);
        return body + "-" + dv;
    }

    public static boolean isValid(String rawRut) {
        String normalized = normalize(rawRut);
        int hyphen = normalized.lastIndexOf('-');
        if (hyphen <= 0 || hyphen == normalized.length() - 1) {
            return false;
        }

        String numberPart = normalized.substring(0, hyphen);
        String dv = normalized.substring(hyphen + 1).toUpperCase();

        if (!numberPart.matches("\\d{7,8}")) {
            return false;
        }

        int sum = 0;
        int multiplier = 2;
        for (int i = numberPart.length() - 1; i >= 0; i--) {
            sum += Character.getNumericValue(numberPart.charAt(i)) * multiplier;
            multiplier = multiplier == 7 ? 2 : multiplier + 1;
        }

        int remainder = 11 - (sum % 11);
        String expectedDv;
        if (remainder == 11) {
            expectedDv = "0";
        } else if (remainder == 10) {
            expectedDv = "K";
        } else {
            expectedDv = String.valueOf(remainder);
        }

        return expectedDv.equalsIgnoreCase(dv);
    }
}
