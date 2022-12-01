package com.chaitin.jar.analyzer.test;

public class CFRTest {
    public static void main(String[] a) {
        String[] args = new String[]{
                "Application.class",
                "--outputpath",
                "1.txt"
        };
        org.benf.cfr.reader.Main.main(args);
    }
}
