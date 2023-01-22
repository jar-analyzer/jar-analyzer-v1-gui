package com.chaitin.jar.analyzer.util;

@SuppressWarnings("unused")
public class JavaVerUtil {
    private static final String javaVersion = System.getProperty("java.version");

    public static boolean isJ8() {
        return javaVersion.startsWith("1.8");
    }

    public static boolean isJ9() {
        return javaVersion.startsWith("9.");
    }

    public static boolean isJ10() {
        return javaVersion.startsWith("10.");
    }

    public static boolean isJ11() {
        return javaVersion.startsWith("11.");
    }

    public static boolean isJ12() {
        return javaVersion.startsWith("12.");
    }

    public static boolean isJ13() {
        return javaVersion.startsWith("13.");
    }

    public static boolean isJ14() {
        return javaVersion.startsWith("14.");
    }

    public static boolean isJ15() {
        return javaVersion.startsWith("15.");
    }

    public static boolean isJ16() {
        return javaVersion.startsWith("16.");
    }

    public static boolean isJ17() {
        return javaVersion.startsWith("17.");
    }

    public static boolean isJ18() {
        return javaVersion.startsWith("18.");
    }

    public static boolean isJ19() {
        return javaVersion.startsWith("19.");
    }
    public static boolean isJ11to14() {
        return isJ11() || isJ12() || isJ13() || isJ14();
    }
}
