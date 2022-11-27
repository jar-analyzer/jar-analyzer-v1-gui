package com.chaitin.jar.analyzer.util;

import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;

public class IOUtil {
    private static final Logger logger = Logger.getLogger(IOUtil.class);

    public static void copy(InputStream inputStream, OutputStream outputStream) {
        try {
            final byte[] buffer = new byte[4096];
            int n;
            while ((n = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, n);
            }
        } catch (Exception e) {
            logger.error("error ", e);
        }
    }
}
