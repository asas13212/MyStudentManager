package studentmanage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 作者：cyt
 * 功能：简易 XLSX 写出工具。
 * 编写时间：2026-05-05
 */
public final class SimpleXlsxWriter {
    /**
     * 作者：cyt
     * 功能：禁止实例化。
     * 编写时间：2026-05-05
     */
    private SimpleXlsxWriter() {
    }

    /**
     * 作者：cyt
     * 功能：写入 XLSX 文件。
     * 编写时间：2026-05-05
     * @param file 输出文件
     * @param sheetName 工作表名称
     * @param headers 表头
     * @param rows 数据行
     * @throws IOException 写入异常
     */
    public static void write(File file, String sheetName, List<String> headers, List<List<Object>> rows) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            putEntry(zos, "[Content_Types].xml", contentTypesXml());
            putEntry(zos, "_rels/.rels", rootRelsXml());
            putEntry(zos, "xl/workbook.xml", workbookXml(sheetName));
            putEntry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            putEntry(zos, "xl/worksheets/sheet1.xml", sheetXml(headers, rows));
        }
    }

    /**
     * 作者：cyt
     * 功能：写入 Zip 条目。
     * 编写时间：2026-05-05
     * @param zos 输出流
     * @param path 路径
     * @param content 内容
     * @throws IOException 写入异常
     */
    private static void putEntry(ZipOutputStream zos, String path, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    /**
     * 作者：cyt
     * 功能：生成 Content Types XML。
     * 编写时间：2026-05-05
     * @return XML 字符串
     */
    private static String contentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "</Types>";
    }

    /**
     * 作者：cyt
     * 功能：生成根关系 XML。
     * 编写时间：2026-05-05
     * @return XML 字符串
     */
    private static String rootRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>";
    }

    /**
     * 作者：cyt
     * 功能：生成工作簿 XML。
     * 编写时间：2026-05-05
     * @param sheetName 工作表名称
     * @return XML 字符串
     */
    private static String workbookXml(String sheetName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets>"
                + "<sheet name=\"" + escapeXml(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/>"
                + "</sheets>"
                + "</workbook>";
    }

    /**
     * 作者：cyt
     * 功能：生成工作簿关系 XML。
     * 编写时间：2026-05-05
     * @return XML 字符串
     */
    private static String workbookRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "</Relationships>";
    }

    /**
     * 作者：cyt
     * 功能：生成工作表 XML。
     * 编写时间：2026-05-05
     * @param headers 表头
     * @param rows 数据行
     * @return XML 字符串
     */
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

    /**
     * 作者：cyt
     * 功能：生成行 XML。
     * 编写时间：2026-05-05
     * @param rowNumber 行号
     * @param values 行数据
     * @return XML 字符串
     */
    private static String rowXml(int rowNumber, List<?> values) {
        StringBuilder xml = new StringBuilder();
        xml.append("<row r=\"").append(rowNumber).append("\">");
        for (int i = 0; i < values.size(); i++) {
            xml.append(cellXml(columnName(i + 1) + rowNumber, values.get(i)));
        }
        xml.append("</row>");
        return xml.toString();
    }

    /**
     * 作者：cyt
     * 功能：生成单元格 XML。
     * 编写时间：2026-05-05
     * @param ref 单元格引用
     * @param value 单元格值
     * @return XML 字符串
     */
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

    /**
     * 作者：cyt
     * 功能：计算列名。
     * 编写时间：2026-05-05
     * @param index 列索引
     * @return 列名
     */
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

    /**
     * 作者：cyt
     * 功能：转义 XML 特殊字符。
     * 编写时间：2026-05-05
     * @param text 原文本
     * @return 转义文本
     */
    private static String escapeXml(String text) {
        String safe = text == null ? "" : text;
        return safe.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    /**
     * 作者：cyt
     * 功能：复制行数据。
     * 编写时间：2026-05-05
     * @param rows 数据行
     * @return 行副本
     */
    public static List<List<Object>> copyRows(List<List<Object>> rows) {
        return new ArrayList<>(rows);
    }
}
