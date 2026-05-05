package studentmanage;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 作者：cyt
 * 功能：学生抽象基类，定义通用属性与成绩行为。
 * 编写时间：2026-05-05
 */
public abstract class Student {
    /** 学生总数统计。作者：cyt；编写时间：2026-05-05 */
    private static int totalCount = 0;

    /** 学号。作者：cyt；编写时间：2026-05-05 */
    private String studentId;
    /** 姓名。作者：cyt；编写时间：2026-05-05 */
    private String name;
    /** 年龄。作者：cyt；编写时间：2026-05-05 */
    private int age;
    /** 班级。作者：cyt；编写时间：2026-05-05 */
    private String className;
    /** 地址。作者：cyt；编写时间：2026-05-05 */
    private Address address;
    /** 成绩集合。作者：cyt；编写时间：2026-05-05 */
    private final Map<String, Double> subjectScores;

    /**
     * 作者：cyt
     * 功能：创建学生对象。
     * 编写时间：2026-05-05
     * @param studentId 学号
     * @param name 姓名
     * @param age 年龄
     * @param className 班级
     * @param address 地址
     * @param subjectScores 成绩
     */
    protected Student(String studentId, String name, int age, String className, Address address, Map<String, Double> subjectScores) {
        this.studentId = studentId;
        this.name = name;
        this.age = age;
        this.className = className;
        this.address = address;
        this.subjectScores = new LinkedHashMap<>(subjectScores);
        // 每次构造学生对象自动计数。
        totalCount++;
    }

    /**
     * 作者：cyt
     * 功能：获取学生总数。
     * 编写时间：2026-05-05
     * @return 总数
     */
    public static int getTotalCount() {
        return totalCount;
    }

    /**
     * 作者：cyt
     * 功能：减少学生总数统计。
     * 编写时间：2026-05-05
     */
    public static void decreaseTotalCount() {
        if (totalCount > 0) {
            totalCount--;
        }
    }

    /**
     * 作者：cyt
     * 功能：获取学号。
     * 编写时间：2026-05-05
     * @return 学号
     */
    public String getStudentId() {
        return studentId;
    }

    /**
     * 作者：cyt
     * 功能：设置学号。
     * 编写时间：2026-05-05
     * @param studentId 学号
     */
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    /**
     * 作者：cyt
     * 功能：获取姓名。
     * 编写时间：2026-05-05
     * @return 姓名
     */
    public String getName() {
        return name;
    }

    /**
     * 作者：cyt
     * 功能：设置姓名。
     * 编写时间：2026-05-05
     * @param name 姓名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 作者：cyt
     * 功能：获取年龄。
     * 编写时间：2026-05-05
     * @return 年龄
     */
    public int getAge() {
        return age;
    }

    /**
     * 作者：cyt
     * 功能：设置年龄。
     * 编写时间：2026-05-05
     * @param age 年龄
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * 作者：cyt
     * 功能：获取班级。
     * 编写时间：2026-05-05
     * @return 班级
     */
    public String getClassName() {
        return className;
    }

    /**
     * 作者：cyt
     * 功能：设置班级。
     * 编写时间：2026-05-05
     * @param className 班级
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * 作者：cyt
     * 功能：获取地址。
     * 编写时间：2026-05-05
     * @return 地址
     */
    public Address getAddress() {
        return address;
    }

    /**
     * 作者：cyt
     * 功能：设置地址。
     * 编写时间：2026-05-05
     * @param address 地址
     */
    public void setAddress(Address address) {
        this.address = address;
    }

    /**
     * 作者：cyt
     * 功能：获取成绩只读视图。
     * 编写时间：2026-05-05
     * @return 成绩Map
     */
    public Map<String, Double> getSubjectScores() {
        // 返回只读视图，避免外部绕过封装直接修改内部成绩Map。
        return Collections.unmodifiableMap(subjectScores);
    }

    /**
     * 作者：cyt
     * 功能：设置单科成绩。
     * 编写时间：2026-05-05
     * @param subject 科目
     * @param score 成绩
     */
    public void setSubjectScore(String subject, double score) {
        subjectScores.put(subject, score);
    }

    /**
     * 作者：cyt
     * 功能：替换全部成绩。
     * 编写时间：2026-05-05
     * @param newScores 新成绩
     */
    public void replaceAllScores(Map<String, Double> newScores) {
        subjectScores.clear();
        subjectScores.putAll(newScores);
    }

    /**
     * 作者：cyt
     * 功能：获取单科成绩。
     * 编写时间：2026-05-05
     * @param subject 科目
     * @return 成绩
     */
    public double getSubjectScore(String subject) {
        return subjectScores.getOrDefault(subject, 0.0);
    }

    /**
     * 作者：cyt
     * 功能：计算总分。
     * 编写时间：2026-05-05
     * @return 总分
     */
    public double getTotalScore() {
        return subjectScores.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /**
     * 作者：cyt
     * 功能：计算平均分。
     * 编写时间：2026-05-05
     * @return 平均分
     */
    public double getAverageScore() {
        if (subjectScores.isEmpty()) {
            return 0.0;
        }
        return getTotalScore() / subjectScores.size();
    }

    /**
     * 作者：cyt
     * 功能：获取学生类型。
     * 编写时间：2026-05-05
     * @return 学生类型
     */
    public abstract StudentType getStudentType();

    /**
     * 作者：cyt
     * 功能：获取类型扩展信息。
     * 编写时间：2026-05-05
     * @return 扩展信息
     */
    protected abstract String getTypeSpecificInfo();

    /**
     * 作者：cyt
     * 功能：输出学生字符串。
     * 编写时间：2026-05-05
     * @return 学生信息文本
     */
    @Override
    public String toString() {
        return String.format(
                "[%s] 学号=%s，姓名=%s，年龄=%d，班级=%s，地址=%s，成绩=%s，总分=%.2f，%s",
                getStudentType(),
                studentId,
                name,
                age,
                className,
                address,
                subjectScores,
                getTotalScore(),
                getTypeSpecificInfo()
        );
    }
}
