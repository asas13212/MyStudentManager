package studentmanage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class SimpleXlsxReader {

    private static final Pattern ROW_PATTERN = Pattern.compile("<row[^>]*>(.*?)</row>", Pattern.DOTALL);
    private static final Pattern CELL_PATTERN = Pattern.compile("<c[^>]*r=\\\"([A-Z]+)([0-9]+)\\\"([^>]*)>(.*?)</c>", Pattern.DOTALL);
    private static final Pattern INLINE_TEXT_PATTERN = Pattern.compile("<t[^>]*>(.*?)</t>", Pattern.DOTALL);
    private static final Pattern VALUE_PATTERN = Pattern.compile("<v>(.*?)</v>", Pattern.DOTALL);

    private SimpleXlsxReader() {
    }

    public static List<List<String>> read(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file, StandardCharsets.UTF_8)) {
            ZipEntry sheet = zip.getEntry("xl/worksheets/sheet1.xml");
            if (sheet == null) {
                return Collections.emptyList();
            }

            String xml;
            try (InputStream in = zip.getInputStream(sheet)) {
                xml = new String(readAll(in), StandardCharsets.UTF_8);
            }

            List<List<String>> rows = new ArrayList<>();
            Matcher rowMatcher = ROW_PATTERN.matcher(xml);
            while (rowMatcher.find()) {
                String rowXml = rowMatcher.group(1);
                rows.add(parseRow(rowXml));
            }
            return rows;
        }
    }

    private static List<String> parseRow(String rowXml) {
        Map<Integer, String> values = new HashMap<>();
        Matcher cellMatcher = CELL_PATTERN.matcher(rowXml);
        int max = -1;
        while (cellMatcher.find()) {
            int colIndex = colToIndex(cellMatcher.group(1));
            String attrs = cellMatcher.group(3);
            String body = cellMatcher.group(4);
            String value = parseCellValue(attrs, body);
            values.put(colIndex, value);
            if (colIndex > max) {
                max = colIndex;
            }
        }

        if (max < 0) {
            return new ArrayList<>();
        }
        List<String> row = new ArrayList<>();
        for (int i = 0; i <= max; i++) {
            row.add(values.getOrDefault(i, ""));
        }
        return row;
    }

    private static String parseCellValue(String attrs, String body) {
        if (attrs != null && attrs.contains("inlineStr")) {
            Matcher m = INLINE_TEXT_PATTERN.matcher(body);
            if (m.find()) {
                return unescapeXml(m.group(1));
            }
            return "";
        }

        Matcher m = VALUE_PATTERN.matcher(body);
        if (m.find()) {
            return unescapeXml(m.group(1));
        }
        return "";
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    private static int colToIndex(String col) {
        int idx = 0;
        for (int i = 0; i < col.length(); i++) {
            idx = idx * 26 + (col.charAt(i) - 'A' + 1);
        }
        return idx - 1;
    }

    private static String unescapeXml(String text) {
        return text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }
}

