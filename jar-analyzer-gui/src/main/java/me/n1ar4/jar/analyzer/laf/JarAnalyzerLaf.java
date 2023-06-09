package me.n1ar4.jar.analyzer.laf;

import com.formdev.flatlaf.FlatDarkLaf;

public class JarAnalyzerLaf extends FlatDarkLaf {
    @SuppressWarnings("all")
    public static boolean setup() {
        return setup(new JarAnalyzerLaf());
    }
}
