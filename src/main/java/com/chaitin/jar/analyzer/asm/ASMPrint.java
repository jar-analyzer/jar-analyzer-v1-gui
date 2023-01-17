package com.chaitin.jar.analyzer.asm;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;

public class ASMPrint {
    public static String getPrint(InputStream is, boolean flag) {
        try {
            int parsingOptions = ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG;
            Printer printer = flag ? new ASMifier() : new Textifier();
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            PrintWriter printWriter = new PrintWriter(bao, true);
            TraceClassVisitor traceClassVisitor = new TraceClassVisitor(null, printer, printWriter);
            new ClassReader(is).accept(traceClassVisitor, parsingOptions);
            return bao.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}