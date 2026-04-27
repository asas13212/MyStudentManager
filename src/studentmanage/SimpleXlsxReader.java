package studentmanage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class SimpleXlsxReader {

    private static final Pattern ROW_PATTERN = Pattern.compile("<row[^>]*>(.*?)</row>", Pattern.DOTALL);
    private static final Pattern CELL_PATTERN = Pattern.compile("<c[^>]*r=\\\"([A-Z]+)([0-9]+)\\\"([^>]*)>(.*?)</c>", Pattern.DOTALL);
    private static final Pattern INLINE_TEXT_PATTERN = Pattern.compile("<t[^>]*>(.*?)</t>", Pattern.DOTALL);
    private static final Pattern VALUE_PATTERN = Pattern.compile("<v>(.*?)</v>", Pattern.DOTALL);
    private static final Pattern SHARED_ITEM_PATTERN = Pattern.compile("<si[^>]*>(.*?)</si>", Pattern.DOTALL);
    private static final Pattern SHEET_TAG_PATTERN = Pattern.compile("<sheet[^>]*name=\\\"(.*?)\\\"[^>]*r:id=\\\"(.*?)\\\"[^>]*/?>", Pattern.DOTALL);
    private static final Pattern REL_PATTERN = Pattern.compile("<Relationship[^>]*Id=\\\"(.*?)\\\"[^>]*Type=\\\"(.*?)\\\"[^>]*Target=\\\"(.*?)\\\"[^>]*/?>", Pattern.DOTALL);

    private SimpleXlsxReader() {
    }

    public static List<List<String>> read(File file) throws IOException {
        try (ZipFile zip = new ZipFile(file, StandardCharsets.UTF_8)) {
            List<String> sheetPaths = resolveWorksheetPaths(zip);
            if (sheetPaths.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> sharedStrings = readSharedStrings(zip);

            List<List<String>> bestRows = Collections.emptyList();
            int bestScore = Integer.MIN_VALUE;
            for (String path : sheetPaths) {
                List<List<String>> rows = readWorksheetRows(zip, path, sharedStrings);
                int score = scoreWorksheet(rows);
                if (score > bestScore) {
                    bestScore = score;
                    bestRows = rows;
                }
            }

            if (!bestRows.isEmpty()) {
                return bestRows;
            }

            // Fallback: return first readable sheet even if scoring is weak.
            for (String path : sheetPaths) {
                List<List<String>> rows = readWorksheetRows(zip, path, sharedStrings);
                if (!rows.isEmpty()) {
                    return rows;
                }
            }
            return Collections.emptyList();
        }
    }

    private static List<String> resolveWorksheetPaths(ZipFile zip) throws IOException {
        List<String> paths = new ArrayList<>();

        String workbookXml = readZipEntryAsString(zip, "xl/workbook.xml");
        String relsXml = readZipEntryAsString(zip, "xl/_rels/workbook.xml.rels");

        if (workbookXml != null && relsXml != null) {
            Map<String, String> relIdToTarget = new HashMap<>();
            Matcher relMatcher = REL_PATTERN.matcher(relsXml);
            while (relMatcher.find()) {
                String relId = relMatcher.group(1);
                String type = relMatcher.group(2);
                String target = relMatcher.group(3);
                if (type != null && type.contains("/worksheet")) {
                    relIdToTarget.put(relId, normalizeWorksheetPath(target));
                }
            }

            Matcher sheetMatcher = SHEET_TAG_PATTERN.matcher(workbookXml);
            while (sheetMatcher.find()) {
                String relId = sheetMatcher.group(2);
                String path = relIdToTarget.get(relId);
                if (path != null && zip.getEntry(path) != null) {
                    paths.add(path);
                }
            }
        }

        if (!paths.isEmpty()) {
            return paths;
        }

        // Fallback: enumerate worksheets directly.
        Set<String> discovered = new LinkedHashSet<>();
        zip.stream().forEach(entry -> {
            String name = entry.getName();
            if (name.startsWith("xl/worksheets/") && name.endsWith(".xml")) {
                discovered.add(name);
            }
        });
        paths.addAll(discovered);
        return paths;
    }

    private static String normalizeWorksheetPath(String target) {
        if (target == null || target.trim().isEmpty()) {
            return "";
        }
        String normalized = target.replace("\\", "/").trim();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("xl/")) {
            return normalized;
        }
        if (normalized.startsWith("worksheets/")) {
            return "xl/" + normalized;
        }
        if (normalized.startsWith("../")) {
            while (normalized.startsWith("../")) {
                normalized = normalized.substring(3);
            }
            return "xl/" + normalized;
        }
        return "xl/" + normalized;
    }

    private static String readZipEntryAsString(ZipFile zip, String entryName) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            return null;
        }
        try (InputStream in = zip.getInputStream(entry)) {
            return new String(readAll(in), StandardCharsets.UTF_8);
        }
    }

    private static List<List<String>> readWorksheetRows(ZipFile zip, String sheetPath, List<String> sharedStrings) throws IOException {
        ZipEntry sheet = zip.getEntry(sheetPath);
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
            rows.add(parseRow(rowXml, sharedStrings));
        }
        return rows;
    }

    private static int scoreWorksheet(List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        int nonEmptyRows = 0;
        int headerHit = 0;
        int scanRows = Math.min(rows.size(), 40);
        for (int r = 0; r < scanRows; r++) {
            List<String> row = rows.get(r);
            if (row == null) {
                continue;
            }
            boolean rowNonEmpty = false;
            for (String cell : row) {
                String value = cell == null ? "" : cell.trim();
                if (!value.isEmpty()) {
                    rowNonEmpty = true;
                    if (containsAny(value, "学号", "考号", "准考证", "学生id", "姓名", "成绩", "分数", "总分", "班级")) {
                        headerHit++;
                    }
                }
            }
            if (rowNonEmpty) {
                nonEmptyRows++;
            }
        }
        return headerHit * 5 + nonEmptyRows;
    }

    private static boolean containsAny(String text, String... keys) {
        String lower = text.toLowerCase();
        for (String key : keys) {
            if (lower.contains(key.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static List<String> readSharedStrings(ZipFile zip) throws IOException {
        ZipEntry shared = zip.getEntry("xl/sharedStrings.xml");
        if (shared == null) {
            return Collections.emptyList();
        }

        String sharedXml;
        try (InputStream in = zip.getInputStream(shared)) {
            sharedXml = new String(readAll(in), StandardCharsets.UTF_8);
        }

        List<String> values = new ArrayList<>();
        Matcher itemMatcher = SHARED_ITEM_PATTERN.matcher(sharedXml);
        while (itemMatcher.find()) {
            String itemXml = itemMatcher.group(1);
            Matcher textMatcher = INLINE_TEXT_PATTERN.matcher(itemXml);
            StringBuilder builder = new StringBuilder();
            while (textMatcher.find()) {
                builder.append(unescapeXml(textMatcher.group(1)));
            }
            values.add(builder.toString());
        }
        return values;
    }

    private static List<String> parseRow(String rowXml, List<String> sharedStrings) {
        Map<Integer, String> values = new HashMap<>();
        Matcher cellMatcher = CELL_PATTERN.matcher(rowXml);
        int max = -1;
        while (cellMatcher.find()) {
            int colIndex = colToIndex(cellMatcher.group(1));
            String attrs = cellMatcher.group(3);
            String body = cellMatcher.group(4);
            String value = parseCellValue(attrs, body, sharedStrings);
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

    private static String parseCellValue(String attrs, String body, List<String> sharedStrings) {
        if (attrs != null && attrs.contains("inlineStr")) {
            Matcher m = INLINE_TEXT_PATTERN.matcher(body);
            if (m.find()) {
                return unescapeXml(m.group(1));
            }
            return "";
        }

        Matcher m = VALUE_PATTERN.matcher(body);
        if (m.find()) {
            String raw = unescapeXml(m.group(1));
            if (attrs != null && attrs.contains("t=\"s\"")) {
                try {
                    int idx = Integer.parseInt(raw.trim());
                    if (idx >= 0 && idx < sharedStrings.size()) {
                        return sharedStrings.get(idx);
                    }
                } catch (NumberFormatException ignore) {
                    return raw;
                }
            }
            return raw;
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

