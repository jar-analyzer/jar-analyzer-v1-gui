package com.chaitin.jar.analyzer.test;

import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;

public class QuiltFlowerTest {
    public static void main(String[] a) {
        String[] args = new String[]{
                "Application.class"
        };
        ConsoleDecompiler.main(args);
    }
}
