package com.chaitin.jar.analyzer;

import com.chaitin.jar.analyzer.form.JarAnalyzerForm;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Application {
    public static void main(String[] args) {
        try {
//            Path outLogPath = new File("jar-analyzer-out.log").toPath();
//            Path errLogPath = new File("jar-analyzer-err.log").toPath();
//            System.setOut(new PrintStream(Files.newOutputStream(outLogPath)));
//            System.setErr(new PrintStream(Files.newOutputStream(errLogPath)));
//
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                try {
//                    Files.delete(errLogPath);
//                } catch (Exception ignored) {
//                }
//                try {
//                    Files.delete(outLogPath);
//                } catch (Exception ignored) {
//                }
//            }));
            JarAnalyzerForm.start();
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
}
