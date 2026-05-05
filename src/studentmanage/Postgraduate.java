package studentmanage;

import java.util.Map;

/**
 * 作者：cyt
 * 功能：研究生实体类。
 * 编写时间：2026-05-05
 */
public class Postgraduate extends Student {
    /** 研究生计数。作者：cyt；编写时间：2026-05-05 */
    private static int count = 0;

    /** 导师信息。作者：cyt；编写时间：2026-05-05 */
    private String supervisor;
    /** 研究方向信息。作者：cyt；编写时间：2026-05-05 */
    private String researchDirection;

    /**
     * 作者：cyt
     * 功能：创建研究生实例。
     * 编写时间：2026-05-05
     * @param studentId 学号
     * @param name 姓名
     * @param age 年龄
     * @param className 班级
     * @param supervisor 导师
     * @param researchDirection 研究方向
     * @param address 地址
     * @param subjectScores 成绩
     */
    public Postgraduate(
            String studentId,
            String name,
            int age,
            String className,
            String supervisor,
            String researchDirection,
            Address address,
            Map<String, Double> subjectScores
    ) {
        super(studentId, name, age, className, address, subjectScores);
        this.supervisor = supervisor;
        this.researchDirection = researchDirection;
        count++;
    }

    /**
     * 作者：cyt
     * 功能：获取研究生人数。
     * 编写时间：2026-05-05
     * @return 研究生人数
     */
    public static int getCount() {
        return count;
    }

    /**
     * 作者：cyt
     * 功能：减少研究生计数。
     * 编写时间：2026-05-05
     */
    public static void decreaseCount() {
        if (count > 0) {
            count--;
        }
    }

    /**
     * 作者：cyt
     * 功能：获取导师信息。
     * 编写时间：2026-05-05
     * @return 导师
     */
    public String getSupervisor() {
        return supervisor;
    }

    /**
     * 作者：cyt
     * 功能：设置导师信息。
     * 编写时间：2026-05-05
     * @param supervisor 导师
     */
    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
    }

    /**
     * 作者：cyt
     * 功能：获取研究方向。
     * 编写时间：2026-05-05
     * @return 研究方向
     */
    public String getResearchDirection() {
        return researchDirection;
    }

    /**
     * 作者：cyt
     * 功能：设置研究方向。
     * 编写时间：2026-05-05
     * @param researchDirection 研究方向
     */
    public void setResearchDirection(String researchDirection) {
        this.researchDirection = researchDirection;
    }

    /**
     * 作者：cyt
     * 功能：获取学生类型。
     * 编写时间：2026-05-05
     * @return 学生类型
     */
    @Override
    public StudentType getStudentType() {
        return StudentType.POSTGRADUATE;
    }

    /**
     * 作者：cyt
     * 功能：获取研究生扩展信息。
     * 编写时间：2026-05-05
     * @return 扩展信息
     */
    @Override
    protected String getTypeSpecificInfo() {
        return "导师=" + supervisor + "，研究方向=" + researchDirection;
    }
}
