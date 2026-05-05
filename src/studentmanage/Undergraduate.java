package studentmanage;

import java.util.Map;

/**
 * 作者：cyt
 * 功能：本科生实体类。
 * 编写时间：2026-05-05
 */
public class Undergraduate extends Student {
    /** 本科生计数。作者：cyt；编写时间：2026-05-05 */
    private static int count = 0;

    /** 专业信息。作者：cyt；编写时间：2026-05-05 */
    private String major;

    /**
     * 作者：cyt
     * 功能：创建本科生实例。
     * 编写时间：2026-05-05
     * @param studentId 学号
     * @param name 姓名
     * @param age 年龄
     * @param className 班级
     * @param major 专业
     * @param address 地址
     * @param subjectScores 成绩
     */
    public Undergraduate(
            String studentId,
            String name,
            int age,
            String className,
            String major,
            Address address,
            Map<String, Double> subjectScores
    ) {
        super(studentId, name, age, className, address, subjectScores);
        this.major = major;
        count++;
    }

    /**
     * 作者：cyt
     * 功能：获取本科生人数。
     * 编写时间：2026-05-05
     * @return 本科生人数
     */
    public static int getCount() {
        return count;
    }

    /**
     * 作者：cyt
     * 功能：减少本科生计数。
     * 编写时间：2026-05-05
     */
    public static void decreaseCount() {
        if (count > 0) {
            count--;
        }
    }

    /**
     * 作者：cyt
     * 功能：获取专业。
     * 编写时间：2026-05-05
     * @return 专业
     */
    public String getMajor() {
        return major;
    }

    /**
     * 作者：cyt
     * 功能：设置专业。
     * 编写时间：2026-05-05
     * @param major 专业
     */
    public void setMajor(String major) {
        this.major = major;
    }

    /**
     * 作者：cyt
     * 功能：获取学生类型。
     * 编写时间：2026-05-05
     * @return 学生类型
     */
    @Override
    public StudentType getStudentType() {
        return StudentType.UNDERGRADUATE;
    }

    /**
     * 作者：cyt
     * 功能：获取本科生扩展信息。
     * 编写时间：2026-05-05
     * @return 扩展信息
     */
    @Override
    protected String getTypeSpecificInfo() {
        return "专业=" + major;
    }
}
