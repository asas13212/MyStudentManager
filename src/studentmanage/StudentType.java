package studentmanage;

/**
 * 作者：cyt
 * 功能：定义学生类型枚举。
 * 编写时间：2026-05-05
 */
public enum StudentType {
    /** 本科生类型。作者：cyt；编写时间：2026-05-05 */
    UNDERGRADUATE("本科生"),
    /** 研究生类型。作者：cyt；编写时间：2026-05-05 */
    POSTGRADUATE("研究生");

    /** 显示名称。作者：cyt；编写时间：2026-05-05 */
    private final String displayName;

    /**
     * 作者：cyt
     * 功能：创建学生类型显示名称。
     * 编写时间：2026-05-05
     * @param displayName 显示名称
     */
    StudentType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 作者：cyt
     * 功能：获取类型显示名称。
     * 编写时间：2026-05-05
     * @return 显示名称
     */
    @Override
    public String toString() {
        return displayName;
    }
}
