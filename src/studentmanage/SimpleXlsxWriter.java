package studentmanage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class SimpleXlsxWriter {
    private SimpleXlsxWriter() {
    }

    public static void write(File file, String sheetName, List<String> headers, List<List<Object>> rows) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            putEntry(zos, "[Content_Types].xml", contentTypesXml());
            putEntry(zos, "_rels/.rels", rootRelsXml());
            putEntry(zos, "xl/workbook.xml", workbookXml(sheetName));
            putEntry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            putEntry(zos, "xl/worksheets/sheet1.xml", sheetXml(headers, rows));
        }
    }

    private static void putEntry(ZipOutputStream zos, String path, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static String contentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "</Types>";
    }

    private static String rootRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    private static String workbookXml(String sheetName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets>"
                + "<sheet name=\"" + escapeXml(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/>"
                + "</sheets>"
                + "</workbook>";
    }

    private static String workbookRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "</Relationships>";
    }

    private static String sheetXml(List<String> headers, List<List<Object>> rows) {
        int lastRow = rows.size() + 1;
        int lastCol = headers.size();
        String dimension = lastCol <= 0 ? "A1" : "A1:" + columnName(lastCol) + Math.max(1, lastRow);

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        xml.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ");
        xml.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
        xml.append("<dimension ref=\"").append(dimension).append("\"/>");
        xml.append("<sheetData>");

        xml.append(rowXml(1, headers));
        for (int i = 0; i < rows.size(); i++) {
            xml.append(rowXml(i + 2, rows.get(i)));
        }

        xml.append("</sheetData>");
        xml.append("</worksheet>");
        return xml.toString();
    }

    private static String rowXml(int rowNumber, List<?> values) {
        StringBuilder xml = new StringBuilder();
        xml.append("<row r=\"").append(rowNumber).append("\">");
        for (int i = 0; i < values.size(); i++) {
            xml.append(cellXml(columnName(i + 1) + rowNumber, values.get(i)));
        }
        xml.append("</row>");
        return xml.toString();
    }

    private static String cellXml(String ref, Object value) {
        if (value == null) {
            return "<c r=\"" + ref + "\" t=\"inlineStr\"><is><t></t></is></c>";
        }
        if (value instanceof Number) {
            return "<c r=\"" + ref + "\"><v>" + value.toString() + "</v></c>";
        }
        if (value instanceof Boolean) {
            return "<c r=\"" + ref + "\" t=\"b\"><v>" + (((Boolean) value) ? "1" : "0") + "</v></c>";
        }
        return "<c r=\"" + ref + "\" t=\"inlineStr\"><is><t>" + escapeXml(String.valueOf(value)) + "</t></is></c>";
    }

    private static String columnName(int index) {
        StringBuilder sb = new StringBuilder();
        int current = index;
        while (current > 0) {
            int rem = (current - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            current = (current - 1) / 26;
        }
        return sb.toString();
    }

    private static String escapeXml(String text) {
        String safe = text == null ? "" : text;
        return safe.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static List<List<Object>> copyRows(List<List<Object>> rows) {
        return new ArrayList<>(rows);
    }
}

