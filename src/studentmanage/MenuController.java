package studentmanage;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 作者：cyt
 * 功能：业务控制器，封装界面层调用的业务逻辑。
 * 编写时间：2026-05-05
 */
public class MenuController {
    /** 学生业务服务。作者：cyt；编写时间：2026-05-05 */
    private final StudentService service;

    /**
     * 作者：cyt
     * 功能：创建控制器。
     * 编写时间：2026-05-05
     * @param service 学生业务服务
     */
    public MenuController(StudentService service) {
        this.service = service;
    }

    /**
     * 作者：cyt
     * 功能：新增学生。
     * 编写时间：2026-05-05
     * @param data 表单数据
     * @return 操作结果
     */
    public OperationResult addStudent(StudentFormData data) {
        if (service.isFull()) {
            return OperationResult.fail("新增失败：存储已满（" + service.getCurrentSize() + "/" + service.getCapacity() + "）。");
        }

        Student student;
        if (data.type == StudentType.UNDERGRADUATE) {
            student = new Undergraduate(data.id, data.name, data.age, data.className, data.major, data.address, data.scores);
        } else {
            student = new Postgraduate(data.id, data.name, data.age, data.className, data.supervisor, data.direction, data.address, data.scores);
        }

        if (!service.addStudent(student)) {
            rollbackCounters(student);
            return OperationResult.fail("新增失败：学号重复，或仓储容量已满。");
        }
        return OperationResult.ok("学生信息已保存。");
    }

    /**
     * 作者：cyt
     * 功能：更新学生信息。
     * 编写时间：2026-05-05
     * @param data 表单数据
     * @return 操作结果
     */
    public OperationResult updateStudent(StudentFormData data) {
        Optional<Student> existing = service.findById(data.id);
        if (existing.isEmpty()) {
            return OperationResult.fail("修改失败：未找到学号为 " + data.id + " 的学生。");
        }
        if (existing.get().getStudentType() != data.type) {
            return OperationResult.fail("修改失败：不支持跨类型变更，请保持与原类型一致。");
        }

        boolean updated = service.updateStudentBasicInfo(
                data.id,
                data.name,
                data.age,
                data.className,
                data.address,
                data.scores,
                data.type == StudentType.UNDERGRADUATE ? data.major : null,
                data.type == StudentType.POSTGRADUATE ? data.supervisor : null,
                data.type == StudentType.POSTGRADUATE ? data.direction : null
        );
        if (!updated) {
            return OperationResult.fail("修改失败。请稍后重试。");
        }
        return OperationResult.ok("学生信息已更新。");
    }

    /**
     * 作者：cyt
     * 功能：按学号删除学生。
     * 编写时间：2026-05-05
     * @param studentId 学号
     * @return 操作结果
     */
    public OperationResult deleteStudentById(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            return OperationResult.fail("删除失败：学号不能为空。");
        }
        if (service.isEmpty()) {
            return OperationResult.fail("删除失败：当前没有学生数据。");
        }

        boolean deleted = service.deleteStudentById(studentId.trim());
        if (!deleted) {
            return OperationResult.fail("删除失败：未找到学号为 " + studentId.trim() + " 的学生。");
        }
        return OperationResult.ok("学生信息已删除。");
    }

    /**
     * 作者：cyt
     * 功能：清空全部学生数据。
     * 编写时间：2026-05-05
     * @return 操作结果
     */
    public OperationResult clearAllStudents() {
        if (service.isEmpty()) {
            return OperationResult.fail("当前没有学生数据可清空。");
        }
        service.clearAllStudents();
        return OperationResult.ok("已清空全部学生数据。");
    }

    /**
     * 作者：cyt
     * 功能：浏览全部学生。
     * 编写时间：2026-05-05
     * @return 学生列表
     */
    public List<Student> browseAllStudents() {
        return service.listAll();
    }

    /**
     * 作者：cyt
     * 功能：按类型浏览学生。
     * 编写时间：2026-05-05
     * @param type 学生类型
     * @return 学生列表
     */
    public List<Student> browseByType(StudentType type) {
        return service.listByType(type);
    }

    /**
     * 作者：cyt
     * 功能：查询学生。
     * 编写时间：2026-05-05
     * @param mode 查询方式
     * @param keyword 查询关键词
     * @return 查询结果
     */
    public QueryResult queryStudents(String mode, String keyword) {
        String key = keyword == null ? "" : keyword.trim();
        if (key.isEmpty()) {
            return QueryResult.fail("请输入查询关键字。", Collections.emptyList());
        }

        if ("按学号".equals(mode)) {
            Optional<Student> student = service.findById(key);
            if (student.isPresent()) {
                return QueryResult.ok(null, Collections.singletonList(student.get()));
            }
            return QueryResult.ok("未找到该学号。", Collections.emptyList());
        }

        if ("按姓名".equals(mode)) {
            return QueryResult.ok(null, service.findByName(key));
        }

        if ("按班级".equals(mode)) {
            return QueryResult.ok(null, service.findByClassName(key));
        }

        return QueryResult.fail("查询方式无效。", Collections.emptyList());
    }

    /**
     * 作者：cyt
     * 功能：排序学生列表。
     * 编写时间：2026-05-05
     * @param range 范围
     * @param sortMode 排序方式
     * @param subjectName 科目名称
     * @param ascending 是否升序
     * @return 查询结果
     */
    public QueryResult sortStudents(String range, String sortMode, String subjectName, boolean ascending) {
        List<Student> source = chooseRange(range);
        if (source.isEmpty()) {
            return QueryResult.fail("当前范围内暂无学生数据。", Collections.emptyList());
        }

        SortField field;
        String subject = null;
        if ("按总分".equals(sortMode)) {
            field = SortField.TOTAL_SCORE;
        } else if ("按单科成绩".equals(sortMode)) {
            field = SortField.SUBJECT_SCORE;
            subject = subjectName == null ? "" : subjectName.trim();
            if (subject.isEmpty()) {
                return QueryResult.fail("按单科成绩排序时必须填写科目名称。", Collections.emptyList());
            }
        } else {
            field = SortField.STUDENT_ID;
        }

        return QueryResult.ok(null, service.sortStudents(source, field, subject, ascending));
    }

    /**
     * 作者：cyt
     * 功能：生成统计文本。
     * 编写时间：2026-05-05
     * @return 统计文本
     */
    public String buildStatisticsText() {
        return String.format(
                "本科生人数：%d\n研究生人数：%d\n学生总人数：%d\n存储使用情况：%d/%d",
                service.getUndergraduateCount(),
                service.getPostgraduateCount(),
                service.getTotalCount(),
                service.getCurrentSize(),
                service.getCapacity()
        );
    }

    /**
     * 作者：cyt
     * 功能：生成关于文本。
     * 编写时间：2026-05-05
     * @return 关于文本
     */
    public String buildAboutText() {
        return "程序名称：学生信息管理系统\n"
                + "版本号：2.1.0(JavaFX)\n"
                + "完成时间：" + LocalDate.now() + "\n"
                + "姓名：曹宇天\n"
                + "学号：8002125290\n"
                + "班级：软件工程2510班";
    }

    /**
     * 作者：cyt
     * 功能：根据范围选择学生列表。
     * 编写时间：2026-05-05
     * @param range 范围
     * @return 学生列表
     */
    private List<Student> chooseRange(String range) {
        if ("仅本科生".equals(range)) {
            return service.listByType(StudentType.UNDERGRADUATE);
        }
        if ("仅研究生".equals(range)) {
            return service.listByType(StudentType.POSTGRADUATE);
        }
        return service.listAll();
    }

    /**
     * 作者：cyt
     * 功能：回滚计数器。
     * 编写时间：2026-05-05
     * @param student 学生对象
     */
    private void rollbackCounters(Student student) {
        Student.decreaseTotalCount();
        if (student instanceof Undergraduate) {
            Undergraduate.decreaseCount();
        } else if (student instanceof Postgraduate) {
            Postgraduate.decreaseCount();
        }
    }

    /**
     * 作者：cyt
     * 功能：操作结果封装。
     * 编写时间：2026-05-05
     */
    public static class OperationResult {
        /** 是否成功。作者：cyt；编写时间：2026-05-05 */
        private final boolean success;
        /** 提示信息。作者：cyt；编写时间：2026-05-05 */
        private final String message;

        /**
         * 作者：cyt
         * 功能：创建操作结果。
         * 编写时间：2026-05-05
         * @param success 是否成功
         * @param message 提示信息
         */
        private OperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        /**
         * 作者：cyt
         * 功能：创建成功结果。
         * 编写时间：2026-05-05
         * @param message 提示信息
         * @return 成功结果
         */
        public static OperationResult ok(String message) {
            return new OperationResult(true, message);
        }

        /**
         * 作者：cyt
         * 功能：创建失败结果。
         * 编写时间：2026-05-05
         * @param message 提示信息
         * @return 失败结果
         */
        public static OperationResult fail(String message) {
            return new OperationResult(false, message);
        }

        /**
         * 作者：cyt
         * 功能：判断是否成功。
         * 编写时间：2026-05-05
         * @return 是否成功
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * 作者：cyt
         * 功能：获取提示信息。
         * 编写时间：2026-05-05
         * @return 提示信息
         */
        public String getMessage() {
            return message;
        }
    }

    /**
     * 作者：cyt
     * 功能：查询结果封装。
     * 编写时间：2026-05-05
     */
    public static class QueryResult {
        /** 是否成功。作者：cyt；编写时间：2026-05-05 */
        private final boolean success;
        /** 提示信息。作者：cyt；编写时间：2026-05-05 */
        private final String message;
        /** 学生列表。作者：cyt；编写时间：2026-05-05 */
        private final List<Student> students;

        /**
         * 作者：cyt
         * 功能：创建查询结果。
         * 编写时间：2026-05-05
         * @param success 是否成功
         * @param message 提示信息
         * @param students 学生列表
         */
        private QueryResult(boolean success, String message, List<Student> students) {
            this.success = success;
            this.message = message;
            this.students = students;
        }

        /**
         * 作者：cyt
         * 功能：创建成功结果。
         * 编写时间：2026-05-05
         * @param message 提示信息
         * @param students 学生列表
         * @return 查询结果
         */
        public static QueryResult ok(String message, List<Student> students) {
            return new QueryResult(true, message, students);
        }

        /**
         * 作者：cyt
         * 功能：创建失败结果。
         * 编写时间：2026-05-05
         * @param message 提示信息
         * @param students 学生列表
         * @return 查询结果
         */
        public static QueryResult fail(String message, List<Student> students) {
            return new QueryResult(false, message, students);
        }

        /**
         * 作者：cyt
         * 功能：判断是否成功。
         * 编写时间：2026-05-05
         * @return 是否成功
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * 作者：cyt
         * 功能：获取提示信息。
         * 编写时间：2026-05-05
         * @return 提示信息
         */
        public String getMessage() {
            return message;
        }

        /**
         * 作者：cyt
         * 功能：获取学生列表。
         * 编写时间：2026-05-05
         * @return 学生列表
         */
        public List<Student> getStudents() {
            return students;
        }
    }

    /**
     * 作者：cyt
     * 功能：表单数据封装。
     * 编写时间：2026-05-05
     */
    public static class StudentFormData {
        /** 学生类型。作者：cyt；编写时间：2026-05-05 */
        private final StudentType type;
        /** 学号。作者：cyt；编写时间：2026-05-05 */
        private final String id;
        /** 姓名。作者：cyt；编写时间：2026-05-05 */
        private final String name;
        /** 年龄。作者：cyt；编写时间：2026-05-05 */
        private final int age;
        /** 班级。作者：cyt；编写时间：2026-05-05 */
        private final String className;
        /** 地址。作者：cyt；编写时间：2026-05-05 */
        private final Address address;
        /** 成绩。作者：cyt；编写时间：2026-05-05 */
        private final Map<String, Double> scores;
        /** 专业。作者：cyt；编写时间：2026-05-05 */
        private final String major;
        /** 导师。作者：cyt；编写时间：2026-05-05 */
        private final String supervisor;
        /** 研究方向。作者：cyt；编写时间：2026-05-05 */
        private final String direction;

        /**
         * 作者：cyt
         * 功能：创建表单数据对象。
         * 编写时间：2026-05-05
         * @param type 类型
         * @param id 学号
         * @param name 姓名
         * @param age 年龄
         * @param className 班级
         * @param address 地址
         * @param scores 成绩
         * @param major 专业
         * @param supervisor 导师
         * @param direction 研究方向
         */
        public StudentFormData(
                StudentType type,
                String id,
                String name,
                int age,
                String className,
                Address address,
                Map<String, Double> scores,
                String major,
                String supervisor,
                String direction
        ) {
            this.type = type;
            this.id = id;
            this.name = name;
            this.age = age;
            this.className = className;
            this.address = address;
            this.scores = scores;
            this.major = major;
            this.supervisor = supervisor;
            this.direction = direction;
        }
    }
}

