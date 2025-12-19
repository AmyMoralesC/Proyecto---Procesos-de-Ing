package Controller;

import java.io.*;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ServletContext;

public class TemplateUtil {

    public static String load(ServletContext ctx, String path) {
        try (InputStream is = ctx.getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("No existe plantilla: " + path);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int r;
            while ((r = is.read(buf)) != -1) {
                bos.write(buf, 0, r);
            }

            return bos.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo plantilla: " + path, e);
        }
    }

    public static String replace(String html, String key, String value) {
        return html.replace(key, value == null ? "" : value);
    }

    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");

    }
    public static String url(String s) {
    try {
        return java.net.URLEncoder.encode(s == null ? "" : s, java.nio.charset.StandardCharsets.UTF_8);
    } catch (Exception e) {
        return "";
    }
}

}
