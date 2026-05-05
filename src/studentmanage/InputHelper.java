package studentmanage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 作者：cyt
 * 功能：控制台输入辅助工具。
 * 编写时间：2026-05-05
 */
public class InputHelper {
    /** 控制台输入器。作者：cyt；编写时间：2026-05-05 */
    private final Scanner scanner;

    /**
     * 作者：cyt
     * 功能：创建输入辅助对象。
     * 编写时间：2026-05-05
     * @param scanner 输入扫描器
     */
    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    /**
     * 作者：cyt
     * 功能：读取非空字符串。
     * 编写时间：2026-05-05
     * @param prompt 提示文本
     * @return 非空输入
     */
    public String readNonBlank(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            // 统一做非空校验，所有输入方法复用此能力。
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("输入不能为空。");
        }
    }

    /**
     * 作者：cyt
     * 功能：读取指定范围内整数。
     * 编写时间：2026-05-05
     * @param prompt 提示文本
     * @param min 最小值
     * @param max 最大值
     * @return 合法整数
     */
    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            String value = readNonBlank(prompt);
            try {
                int parsed = Integer.parseInt(value);
                // 范围校验失败时继续循环，保证返回值始终合法。
                if (parsed < min || parsed > max) {
                    System.out.println("输入范围必须在 " + min + " 到 " + max + " 之间。");
                    continue;
                }
                return parsed;
            } catch (NumberFormatException ex) {
                System.out.println("请输入整数。");
            }
        }
    }

    /**
     * 作者：cyt
     * 功能：读取指定范围内小数。
     * 编写时间：2026-05-05
     * @param prompt 提示文本
     * @param min 最小值
     * @param max 最大值
     * @return 合法数值
     */
    public double readDoubleInRange(String prompt, double min, double max) {
        while (true) {
            String value = readNonBlank(prompt);
            try {
                double parsed = Double.parseDouble(value);
                if (parsed < min || parsed > max) {
                    System.out.println("输入范围必须在 " + min + " 到 " + max + " 之间。");
                    continue;
                }
                return parsed;
            } catch (NumberFormatException ex) {
                System.out.println("请输入数字。");
            }
        }
    }

    /**
     * 作者：cyt
     * 功能：读取是/否选项。
     * 编写时间：2026-05-05
     * @param prompt 提示文本
     * @return 是否为“是”
     */
    public boolean readYesNo(String prompt) {
        while (true) {
            String value = readNonBlank(prompt + "（是/否）：").toLowerCase();
            if ("是".equals(value) || "y".equals(value)) {
                return true;
            }
            if ("否".equals(value) || "n".equals(value)) {
                return false;
            }
            System.out.println("请输入“是”或“否”。");
        }
    }

    /**
     * 作者：cyt
     * 功能：读取地址信息。
     * 编写时间：2026-05-05
     * @return 地址对象
     */
    public Address readAddress() {
        String province = readNonBlank("省份：");
        String city = readNonBlank("城市：");
        String street = readNonBlank("街道：");
        String houseNumber = readNonBlank("门牌号：");
        return new Address(province, city, street, houseNumber);
    }

    /**
     * 作者：cyt
     * 功能：读取成绩信息。
     * 编写时间：2026-05-05
     * @return 成绩Map
     */
    public Map<String, Double> readScores() {
        int subjectCount = readIntInRange("请输入科目数量：", 1, 30);
        Map<String, Double> scores = new LinkedHashMap<>();
        // 使用有序Map，后续展示成绩时保持与录入顺序一致。
        for (int i = 1; i <= subjectCount; i++) {
            String subject = readNonBlank("第 " + i + " 门科目名称：");
            double score = readDoubleInRange("第 " + i + " 门科目成绩（0-100）：", 0, 100);
            scores.put(subject, score);
        }
        return scores;
    }
}
