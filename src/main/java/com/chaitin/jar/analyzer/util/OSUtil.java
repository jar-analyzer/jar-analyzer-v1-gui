package com.chaitin.jar.analyzer.util;

public class OSUtil {
    public static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains("windows");
    }
}
