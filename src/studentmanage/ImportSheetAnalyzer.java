package studentmanage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Analyze worksheet structure without relying on fixed header names.
 */
public final class ImportSheetAnalyzer {
	private static final Map<String, List<String>> FIELD_ALIASES = buildFieldAliases();
	private static final Set<String> CORE_FIELD_KEYS = new LinkedHashSet<>(Arrays.asList(
			"id", "name", "type", "age", "class", "major", "supervisor", "direction",
			"province", "city", "street", "house"
	));
	private static final Set<String> DISPLAY_EXTRA_FIELD_KEYS = new LinkedHashSet<>(Arrays.asList(
			"classRank", "gradeRank", "schoolRank", "college"
	));

	private ImportSheetAnalyzer() {
	}

	public static AnalysisResult analyze(List<List<String>> rows) {
		if (rows == null || rows.isEmpty()) {
			return AnalysisResult.fail("文件中没有可读取数据。");
		}

		int headerRowIndex = detectHeaderRowIndex(rows);
		if (headerRowIndex < 0 || headerRowIndex >= rows.size()) {
			return AnalysisResult.fail("未识别到有效表头。");
		}

		int dataStartRow = headerRowIndex + 1;
		List<String> headers = rows.get(headerRowIndex);
		Map<String, Integer> index = buildHeaderIndex(headers);
		int maxCols = detectMaxColumns(rows);
		Map<String, Integer> fieldBindings = buildFieldBindings(headers, maxCols);

		int idColumn = firstBoundOrDefault(fieldBindings.get("id"), resolveIdColumn(index, rows, dataStartRow));
		if (idColumn < 0) {
			return AnalysisResult.fail("未识别到学生唯一标识列（学号/考号/准考证号/学生ID）。");
		}

		int nameColumn = firstBoundOrDefault(fieldBindings.get("name"), resolveNameColumn(index, headers, idColumn));
		if (nameColumn < 0) {
			return AnalysisResult.fail("未识别到姓名列（姓名/学生姓名/名字）。");
		}

		Integer classColumn = firstBoundOrNull(fieldBindings.get("class"), findColumn(index, "班级", "行政班", "班别", "class", "学院"));
		Integer majorColumn = firstBoundOrNull(fieldBindings.get("major"), findColumn(index, "专业", "学科", "方向", "学院"));

		LinkedHashMap<String, Integer> scoreColumns = detectScoreColumns(
				headers,
				index,
				rows,
				maxCols,
				dataStartRow,
				idColumn,
				nameColumn,
				fieldBindings
		);
		if (scoreColumns.isEmpty()) {
			return AnalysisResult.fail("未识别到可用成绩列（总分或数值成绩列）。");
		}

		LinkedHashMap<String, Integer> extraColumns = detectExtraColumns(headers, scoreColumns, fieldBindings);
		List<String> extraHeaders = new ArrayList<>(extraColumns.keySet());

		return AnalysisResult.ok(
				headerRowIndex,
				dataStartRow,
				headers,
				index,
				idColumn,
				nameColumn,
				classColumn,
				majorColumn,
				fieldBindings,
				scoreColumns,
				extraColumns,
				extraHeaders
		);
	}

	private static int firstBoundOrDefault(Integer boundValue, int fallback) {
		return boundValue != null ? boundValue : fallback;
	}

	private static Integer firstBoundOrNull(Integer boundValue, Integer fallback) {
		return boundValue != null ? boundValue : fallback;
	}

	public static boolean shouldSkipDataRow(List<String> row) {
		if (row == null || row.isEmpty()) {
			return true;
		}
		int nonBlank = 0;
		String first = "";
		for (String cell : row) {
			String value = normalizeText(cell);
			if (!value.isEmpty()) {
				nonBlank++;
				if (first.isEmpty()) {
					first = value;
				}
			}
		}
		if (nonBlank == 0) {
			return true;
		}
		String firstNorm = normalizeHeader(first);
		return nonBlank == 1 && (firstNorm.contains("说明")
				|| firstNorm.contains("备注")
				|| firstNorm.contains("单位")
				|| firstNorm.contains("标题"));
	}

	public static String normalizeText(String text) {
		if (text == null) {
			return "";
		}
		return text.replace('\u3000', ' ').trim();
	}


	private static int detectHeaderRowIndex(List<List<String>> rows) {
		if (rows == null || rows.isEmpty()) {
			return -1;
		}
		List<String> firstRow = rows.get(0);
		if (firstRow != null && countNonBlank(firstRow) >= 3 && scoreHeaderRow(firstRow) >= 2) {
			return 0;
		}
		int scanLimit = Math.min(rows.size(), 60);
		int bestIndex = -1;
		int bestScore = Integer.MIN_VALUE;
		for (int i = 0; i < scanLimit; i++) {
			int score = scoreHeaderRow(rows.get(i));
			if (score > bestScore) {
				bestScore = score;
				bestIndex = i;
			}
		}
		if (bestScore >= 3) {
			return bestIndex;
		}
		for (int i = 1; i < rows.size(); i++) {
			if (!isAllBlank(rows.get(i))) {
				return i;
			}
		}
		return 0;
	}

	private static boolean isAllBlank(List<String> row) {
		if (row == null || row.isEmpty()) {
			return true;
		}
		for (String cell : row) {
			if (!normalizeText(cell).isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private static int countNonBlank(List<String> row) {
		if (row == null || row.isEmpty()) {
			return 0;
		}
		int count = 0;
		for (String cell : row) {
			if (!normalizeText(cell).isEmpty()) {
				count++;
			}
		}
		return count;
	}

	private static int inferLikelyNameColumn(List<String> headers, int idColumn) {
		int best = -1;
		int bestScore = Integer.MIN_VALUE;
		for (int i = 0; i < headers.size(); i++) {
			if (i == idColumn) {
				continue;
			}
			String raw = normalizeText(getCell(headers, i));
			if (raw.isEmpty()) {
				continue;
			}
			String normalized = normalizeHeader(raw);
			if (isScoreLikeHeader(normalized) || isCoreMetaHeader(normalized)) {
				continue;
			}
			int score = 0;
			if (containsAny(normalized, "姓名", "名字", "name")) {
				score += 6;
			}
			if (containsAny(normalized, "学生", "考生")) {
				score += 2;
			}
			if (!isNumericLike(raw)) {
				score += 1;
			}
			if (score > bestScore) {
				bestScore = score;
				best = i;
			}
		}
		return best;
	}

	private static int scoreHeaderRow(List<String> row) {
		if (row == null || row.isEmpty()) {
			return Integer.MIN_VALUE;
		}
		int score = 0;
		for (String cell : row) {
			String h = normalizeHeader(cell);
			if (h.isEmpty()) {
				continue;
			}
			if (containsAny(h, "学号", "考号", "准考证", "studentid", "id")) {
				score += 4;
			}
			if (containsAny(h, "姓名", "学生姓名", "名字", "name")) {
				score += 4;
			}
			if (containsAny(h, "总分", "成绩", "分数", "语文", "数学", "英语", "物理", "化学", "生物", "政治", "历史", "地理")) {
				score += 2;
			}
			if (containsAny(h, "班级", "专业", "学院")) {
				score += 1;
			}
		}
		return score;
	}

	private static int resolveIdColumn(Map<String, Integer> index, List<List<String>> rows, int dataStartRow) {
		Integer direct = findColumn(index, "学号", "考号", "准考证号", "学生id", "studentid", "id");
		if (direct != null) {
			return direct;
		}
		int inferred = inferUniqueLikeColumn(rows, dataStartRow, null);
		if (inferred >= 0) {
			return inferred;
		}
		return inferBestDistinctColumn(rows, dataStartRow, null);
	}

	private static int resolveNameColumn(Map<String, Integer> index, List<String> headers, int idColumn) {
		Integer direct = findColumn(index, "姓名", "学生姓名", "名字", "name");
		if (direct != null) {
			return direct;
		}
		for (int i = 0; i < headers.size(); i++) {
			if (i == idColumn) {
				continue;
			}
			String h = normalizeHeader(headers.get(i));
			if (containsAny(h, "姓名", "名字", "name")) {
				return i;
			}
		}
		return inferLikelyNameColumn(headers, idColumn);
	}

	private static LinkedHashMap<String, Integer> detectScoreColumns(List<String> headers,
											  Map<String, Integer> index,
											  List<List<String>> rows,
											  int maxCols,
											  int dataStartRow,
											  int idColumn,
											  int nameColumn,
											  Map<String, Integer> fieldBindings) {
		LinkedHashMap<String, Integer> columns = new LinkedHashMap<>();

		Integer totalCol = findColumn(index, "总分", "总成绩", "总成绩分", "score", "成绩", "分数");
		if (totalCol != null) {
			columns.put(resolveHeaderTitle(headers, totalCol, "总分"), totalCol);
		}

		Set<Integer> exclude = new HashSet<>();
		exclude.add(idColumn);
		exclude.add(nameColumn);
		for (Map.Entry<String, Integer> entry : fieldBindings.entrySet()) {
			if (CORE_FIELD_KEYS.contains(entry.getKey())) {
				exclude.add(entry.getValue());
			}
		}

		int bestNumericCol = -1;
		double bestRatio = 0.0;
		for (int col = 0; col < maxCols; col++) {
			if (exclude.contains(col) || columns.containsValue(col)) {
				continue;
			}
			String header = resolveHeaderTitle(headers, col, "成绩" + (col + 1));
			String normalized = normalizeHeader(header);
			if (normalized.isEmpty() || isCoreMetaHeader(normalized) || isRankingLikeHeader(normalized)) {
				continue;
			}

			double ratio = columnNumericRatio(rows, dataStartRow, col);
			if ((isScoreLikeHeader(normalized) && ratio >= 0.35) || ratio >= 0.80) {
				columns.put(header, col);
			}
			if (ratio > bestRatio) {
				bestRatio = ratio;
				bestNumericCol = col;
			}
		}

		if (columns.isEmpty() && bestNumericCol >= 0 && bestRatio >= 0.30) {
			String title = resolveHeaderTitle(headers, bestNumericCol, "成绩");
			columns.put(title, bestNumericCol);
		}

		return columns;
	}

	private static LinkedHashMap<String, Integer> detectExtraColumns(List<String> headers,
													  LinkedHashMap<String, Integer> scoreColumns,
													  Map<String, Integer> fieldBindings) {
		LinkedHashMap<String, Integer> extras = new LinkedHashMap<>();
		Set<Integer> scoreIndexes = new HashSet<>(scoreColumns.values());
		for (Map.Entry<String, Integer> binding : fieldBindings.entrySet()) {
			String key = binding.getKey();
			int col = binding.getValue();
			if (!DISPLAY_EXTRA_FIELD_KEYS.contains(key) || scoreIndexes.contains(col)) {
				continue;
			}
			String displayName = resolveExtraDisplayName(key, resolveHeaderTitle(headers, col, "字段" + (col + 1)));
			extras.put(ensureUniqueHeader(extras, displayName), col);
		}
		return extras;
	}

	private static String resolveExtraDisplayName(String key, String fallback) {
		if ("classRank".equals(key)) {
			return "班级排名";
		}
		if ("gradeRank".equals(key)) {
			return "年级排名";
		}
		if ("schoolRank".equals(key)) {
			return "校排名";
		}
		if ("college".equals(key)) {
			return "学院";
		}
		return fallback;
	}

	private static String resolveHeaderTitle(List<String> headers, int col, String fallback) {
		String value = normalizeText(getCell(headers, col));
		return value.isEmpty() ? fallback : value;
	}

	private static String ensureUniqueHeader(Map<String, Integer> existing, String base) {
		String candidate = base;
		int idx = 2;
		while (existing.containsKey(candidate)) {
			candidate = base + "_" + idx;
			idx++;
		}
		return candidate;
	}

	private static double columnNumericRatio(List<List<String>> rows, int dataStartRow, int col) {
		int seen = 0;
		int numeric = 0;
		for (int r = dataStartRow; r < rows.size(); r++) {
			String value = normalizeText(getCell(rows.get(r), col));
			if (value.isEmpty() || "-".equals(value) || "--".equals(value)) {
				continue;
			}
			seen++;
			if (isNumericLike(value)) {
				numeric++;
			}
		}
		if (seen == 0) {
			return 0.0;
		}
		return numeric * 1.0 / seen;
	}

	private static int inferUniqueLikeColumn(List<List<String>> rows, int dataStartRow, Integer excludeCol) {
		int maxCols = detectMaxColumns(rows);
		int bestCol = -1;
		double bestDistinctRatio = 0.0;
		for (int c = 0; c < maxCols; c++) {
			if (excludeCol != null && c == excludeCol) {
				continue;
			}
			Set<String> distinct = new HashSet<>();
			int valid = 0;
			for (int r = dataStartRow; r < rows.size(); r++) {
				String value = normalizeText(getCell(rows.get(r), c));
				if (value.isEmpty()) {
					continue;
				}
				valid++;
				distinct.add(value);
			}
			if (valid >= 2) {
				double ratio = distinct.size() * 1.0 / valid;
				if (ratio > bestDistinctRatio && ratio >= 0.85) {
					bestDistinctRatio = ratio;
					bestCol = c;
				}
			}
		}
		return bestCol;
	}

	private static int inferBestDistinctColumn(List<List<String>> rows, int dataStartRow, Integer excludeCol) {
		int maxCols = detectMaxColumns(rows);
		int bestCol = -1;
		int bestDistinct = -1;
		for (int c = 0; c < maxCols; c++) {
			if (excludeCol != null && c == excludeCol) {
				continue;
			}
			Set<String> distinct = new HashSet<>();
			int valid = 0;
			for (int r = dataStartRow; r < rows.size(); r++) {
				String value = normalizeText(getCell(rows.get(r), c));
				if (value.isEmpty()) {
					continue;
				}
				valid++;
				distinct.add(value);
			}
			if (valid >= 2 && distinct.size() > bestDistinct) {
				bestDistinct = distinct.size();
				bestCol = c;
			}
		}
		return bestCol;
	}

	private static int detectMaxColumns(List<List<String>> rows) {
		int max = 0;
		for (List<String> row : rows) {
			if (row != null) {
				max = Math.max(max, row.size());
			}
		}
		return max;
	}

	private static String getCell(List<String> row, int idx) {
		if (row == null || idx < 0 || idx >= row.size()) {
			return "";
		}
		String value = row.get(idx);
		return value == null ? "" : value;
	}

	private static Map<String, Integer> buildHeaderIndex(List<String> headers) {
		Map<String, Integer> index = new HashMap<>();
		for (int i = 0; i < headers.size(); i++) {
			String h = normalizeText(headers.get(i));
			if (!h.isEmpty()) {
				index.put(h, i);
				index.put(normalizeHeader(h), i);
			}
		}
		return index;
	}

	private static Map<String, Integer> buildFieldBindings(List<String> headers, int maxCols) {
		Map<String, Integer> bindings = new LinkedHashMap<>();
		for (Map.Entry<String, List<String>> spec : FIELD_ALIASES.entrySet()) {
			String key = spec.getKey();
			List<String> aliases = spec.getValue();
			int bestCol = -1;
			int bestScore = Integer.MIN_VALUE;
			for (int col = 0; col < maxCols; col++) {
				String header = resolveHeaderTitle(headers, col, "");
				String normalized = normalizeHeader(header);
				if (normalized.isEmpty()) {
					continue;
				}
				int score = scoreAliasMatch(normalized, aliases);
				if (score > bestScore) {
					bestScore = score;
					bestCol = col;
				}
			}
			if (bestCol >= 0 && bestScore >= 3) {
				bindings.put(key, bestCol);
			}
		}
		return bindings;
	}

	private static int scoreAliasMatch(String header, List<String> aliases) {
		int score = Integer.MIN_VALUE;
		for (String alias : aliases) {
			String normalizedAlias = normalizeHeader(alias);
			if (normalizedAlias.isEmpty()) {
				continue;
			}
			if (header.equals(normalizedAlias)) {
				score = Math.max(score, 8);
			} else if (header.contains(normalizedAlias)) {
				score = Math.max(score, 5);
			}
		}
		return score == Integer.MIN_VALUE ? 0 : score;
	}

	private static Map<String, List<String>> buildFieldAliases() {
		Map<String, List<String>> aliases = new LinkedHashMap<>();
		aliases.put("id", Arrays.asList("学号", "考号", "准考证号", "学生id", "studentid", "id"));
		aliases.put("name", Arrays.asList("姓名", "学生姓名", "名字", "name"));
		aliases.put("type", Arrays.asList("类型", "学生类型", "类别", "身份"));
		aliases.put("age", Arrays.asList("年龄", "age"));
		aliases.put("class", Arrays.asList("班级", "行政班", "班别", "class"));
		aliases.put("major", Arrays.asList("专业", "学科", "学院"));
		aliases.put("supervisor", Arrays.asList("导师", "指导教师", "班主任"));
		aliases.put("direction", Arrays.asList("研究方向", "科研方向", "培养方向"));
		aliases.put("province", Arrays.asList("省份", "省"));
		aliases.put("city", Arrays.asList("城市", "市"));
		aliases.put("street", Arrays.asList("街道", "详细地址", "地址"));
		aliases.put("house", Arrays.asList("门牌号", "门牌"));
		aliases.put("classRank", Arrays.asList("班级排名", "班排名", "班内排名"));
		aliases.put("gradeRank", Arrays.asList("年级排名", "级排名"));
		aliases.put("schoolRank", Arrays.asList("校排名", "总排名"));
		aliases.put("college", Arrays.asList("学院"));
		return aliases;
	}

	/**
	 * 作者：cyt
	 * 功能：按同义词查找列。
	 * 编写时间：2026-05-05
	 * @param index 表头索引
	 * @param aliases 同义词
	 * @return 列索引
	 */
	private static Integer findColumn(Map<String, Integer> index, String... aliases) {
		for (String alias : aliases) {
			if (alias == null) {
				continue;
			}
			Integer pos = index.get(alias);
			if (pos != null) {
				return pos;
			}
			pos = index.get(normalizeHeader(alias));
			if (pos != null) {
				return pos;
			}
		}
		return null;
	}

	/**
	 * 作者：cyt
	 * 功能：规范化表头。
	 * 编写时间：2026-05-05
	 * @param header 表头文本
	 * @return 规范文本
	 */
	private static String normalizeHeader(String header) {
		return normalizeText(header).toLowerCase(Locale.ROOT).replace(" ", "");
	}

	/**
	 * 作者：cyt
	 * 功能：判断是否包含关键字。
	 * 编写时间：2026-05-05
	 * @param value 文本
	 * @param keys 关键字
	 * @return 是否包含
	 */
	private static boolean containsAny(String value, String... keys) {
		for (String key : keys) {
			if (value.contains(key.toLowerCase(Locale.ROOT))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 作者：cyt
	 * 功能：判断文本是否可视为数字。
	 * 编写时间：2026-05-05
	 * @param text 文本
	 * @return 是否为数字
	 */
	private static boolean isNumericLike(String text) {
		String value = normalizeText(text).replace(",", "");
		if (value.isEmpty()) {
			return false;
		}
		if (value.contains("[") || value.contains("]") || value.matches(".*[A-Za-z].*")) {
			return false;
		}
		if (value.matches(".*(等级|优秀|良好|中等|及格|不及格|缺考|弃考).*")) {
			return false;
		}
		if (value.matches("-?\\d+(\\.\\d+)?")) {
			return true;
		}
		if (value.matches("-?\\d+(\\.\\d+)?分")) {
			return true;
		}
		String numeric = value.replaceAll("[^0-9.\\-]", "");
		return !numeric.isEmpty() && numeric.matches("-?\\d+(\\.\\d+)?");
	}

	/**
	 * 作者：cyt
	 * 功能：判断表头是否为成绩类。
	 * 编写时间：2026-05-05
	 * @param normalizedHeader 规范表头
	 * @return 是否为成绩类
	 */
	private static boolean isScoreLikeHeader(String normalizedHeader) {
		if (containsAny(normalizedHeader, "等级", "等第", "名次", "排名", "位次")) {
			return false;
		}
		return containsAny(normalizedHeader,
				"分", "成绩", "score", "语文", "数学", "英语", "物理", "化学", "生物", "政治", "历史", "地理");
	}

	/**
	 * 作者：cyt
	 * 功能：判断表头是否为排名类。
	 * 编写时间：2026-05-05
	 * @param normalizedHeader 规范表头
	 * @return 是否为排名类
	 */
	private static boolean isRankingLikeHeader(String normalizedHeader) {
		return containsAny(normalizedHeader,
				"排名", "名次", "位次", "序号", "座号", "考场", "考号", "学号", "编号", "班内");
	}

	/**
	 * 作者：cyt
	 * 功能：判断是否为核心信息表头。
	 * 编写时间：2026-05-05
	 * @param normalized 规范表头
	 * @return 是否为核心信息
	 */
	private static boolean isCoreMetaHeader(String normalized) {
		Set<String> core = new LinkedHashSet<>(Arrays.asList(
				"学号", "考号", "准考证号", "id", "studentid", "学生id",
				"姓名", "学生姓名", "name", "名字",
				"类型", "学生类型", "类别", "身份",
				"年龄", "年级", "age",
				"班级", "行政班", "班别", "class", "专业", "学科", "学院",
				"省份", "省", "城市", "市", "街道", "详细地址", "地址", "门牌号", "门牌",
				"导师", "指导教师", "班主任", "研究方向", "方向"
		));
		return core.contains(normalized);
	}

	public static final class AnalysisResult {
		private final boolean success;
		private final String message;
		private final int headerRowIndex;
		private final int dataStartRow;
		private final List<String> headers;
		private final Map<String, Integer> index;
		private final int idColumn;
		private final int nameColumn;
		private final Integer classColumn;
		private final Integer majorColumn;
		private final Map<String, Integer> fieldBindings;
		private final LinkedHashMap<String, Integer> scoreColumns;
		private final LinkedHashMap<String, Integer> extraColumns;
		private final List<String> extraHeaders;

		private AnalysisResult(boolean success,
							   String message,
							   int headerRowIndex,
							   int dataStartRow,
							   List<String> headers,
							   Map<String, Integer> index,
							   int idColumn,
							   int nameColumn,
							   Integer classColumn,
							   Integer majorColumn,
							   Map<String, Integer> fieldBindings,
							   LinkedHashMap<String, Integer> scoreColumns,
							   LinkedHashMap<String, Integer> extraColumns,
							   List<String> extraHeaders) {
			this.success = success;
			this.message = message;
			this.headerRowIndex = headerRowIndex;
			this.dataStartRow = dataStartRow;
			this.headers = headers;
			this.index = index;
			this.idColumn = idColumn;
			this.nameColumn = nameColumn;
			this.classColumn = classColumn;
			this.majorColumn = majorColumn;
			this.fieldBindings = fieldBindings;
			this.scoreColumns = scoreColumns;
			this.extraColumns = extraColumns;
			this.extraHeaders = extraHeaders;
		}

		private static AnalysisResult fail(String message) {
			return new AnalysisResult(false, message, -1, -1,
					Collections.emptyList(), Collections.emptyMap(), -1, -1,
					null, null, Collections.emptyMap(), new LinkedHashMap<>(), new LinkedHashMap<>(), Collections.emptyList());
		}

		private static AnalysisResult ok(int headerRowIndex,
										 int dataStartRow,
										 List<String> headers,
										 Map<String, Integer> index,
										 int idColumn,
										 int nameColumn,
										 Integer classColumn,
										 Integer majorColumn,
													 Map<String, Integer> fieldBindings,
										 LinkedHashMap<String, Integer> scoreColumns,
													 LinkedHashMap<String, Integer> extraColumns,
										 List<String> extraHeaders) {
			return new AnalysisResult(true, "", headerRowIndex, dataStartRow,
					new ArrayList<>(headers), new HashMap<>(index), idColumn, nameColumn,
													classColumn, majorColumn, new HashMap<>(fieldBindings), new LinkedHashMap<>(scoreColumns),
									new LinkedHashMap<>(extraColumns), new ArrayList<>(extraHeaders));
		}

		public boolean isSuccess() {
			return success;
		}

		public String getMessage() {
			return message;
		}

		public int getHeaderRowIndex() {
			return headerRowIndex;
		}

		public int getDataStartRow() {
			return dataStartRow;
		}

		public List<String> getHeaders() {
			return headers;
		}

		public Map<String, Integer> getIndex() {
			return index;
		}

		public int getIdColumn() {
			return idColumn;
		}

		public int getNameColumn() {
			return nameColumn;
		}

		public Integer getClassColumn() {
			return classColumn;
		}

		public Integer getMajorColumn() {
			return majorColumn;
		}

		public Map<String, Integer> getFieldBindings() {
			return fieldBindings;
		}

		public LinkedHashMap<String, Integer> getScoreColumns() {
			return scoreColumns;
		}

		public LinkedHashMap<String, Integer> getExtraColumns() {
			return extraColumns;
		}

		public List<String> getExtraHeaders() {
			return extraHeaders;
		}
	}
}

