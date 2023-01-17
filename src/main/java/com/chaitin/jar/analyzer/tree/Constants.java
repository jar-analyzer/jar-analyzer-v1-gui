package com.chaitin.jar.analyzer.tree;

public class Constants {
    public final static String osName = System.getProperty("os.name");
    public final static boolean isOSX = osName.equalsIgnoreCase("Mac OS X");
    public final static boolean isLinux = osName.equalsIgnoreCase("Linux");
    public final static boolean isSolaris = osName.equalsIgnoreCase("SunOS");
    public final static boolean isWindows = !(isOSX || isLinux || isSolaris);
}
