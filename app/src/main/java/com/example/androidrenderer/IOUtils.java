package com.example.androidrenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IOUtils {

    public static byte[] loadBuffer(InputStream is, boolean closed) {
        if (is == null) return null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len = -1;
        try {
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
                baos.flush();
            }
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (closed) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }
}
