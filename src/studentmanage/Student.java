package studentmanage;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Student {
    // 记录系统内学生总数（静态成员，满足作业要求）。
    private static int totalCount = 0;

    private String studentId;
    private String name;
    private int age;
    private String className;
    private Address address;
    private final Map<String, Double> subjectScores;

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

    public static int getTotalCount() {
        return totalCount;
    }

    public static void decreaseTotalCount() {
        if (totalCount > 0) {
            totalCount--;
        }
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Map<String, Double> getSubjectScores() {
        // 返回只读视图，避免外部绕过封装直接修改内部成绩Map。
        return Collections.unmodifiableMap(subjectScores);
    }

    public void setSubjectScore(String subject, double score) {
        subjectScores.put(subject, score);
    }

    public void replaceAllScores(Map<String, Double> newScores) {
        subjectScores.clear();
        subjectScores.putAll(newScores);
    }

    public double getSubjectScore(String subject) {
        return subjectScores.getOrDefault(subject, 0.0);
    }

    public double getTotalScore() {
        return subjectScores.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    public double getAverageScore() {
        if (subjectScores.isEmpty()) {
            return 0.0;
        }
        return getTotalScore() / subjectScores.size();
    }

    public abstract StudentType getStudentType();

    protected abstract String getTypeSpecificInfo();

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

