package studentmanage;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MenuController {
    private final StudentService service;

    public MenuController(StudentService service) {
        this.service = service;
    }

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

    public OperationResult clearAllStudents() {
        if (service.isEmpty()) {
            return OperationResult.fail("当前没有学生数据可清空。");
        }
        service.clearAllStudents();
        return OperationResult.ok("已清空全部学生数据。");
    }

    public List<Student> browseAllStudents() {
        return service.listAll();
    }

    public List<Student> browseByType(StudentType type) {
        return service.listByType(type);
    }

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

    public String buildAboutText() {
        return "程序名称：学生信息管理系统\n"
                + "版本号：2.1.0(JavaFX)\n"
                + "完成时间：" + LocalDate.now() + "\n"
                + "姓名：曹宇天\n"
                + "学号：8002125290\n"
                + "班级：软件工程2510班";
    }

    private List<Student> chooseRange(String range) {
        if ("仅本科生".equals(range)) {
            return service.listByType(StudentType.UNDERGRADUATE);
        }
        if ("仅研究生".equals(range)) {
            return service.listByType(StudentType.POSTGRADUATE);
        }
        return service.listAll();
    }

    private void rollbackCounters(Student student) {
        Student.decreaseTotalCount();
        if (student instanceof Undergraduate) {
            Undergraduate.decreaseCount();
        } else if (student instanceof Postgraduate) {
            Postgraduate.decreaseCount();
        }
    }

    public static class OperationResult {
        private final boolean success;
        private final String message;

        private OperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static OperationResult ok(String message) {
            return new OperationResult(true, message);
        }

        public static OperationResult fail(String message) {
            return new OperationResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class QueryResult {
        private final boolean success;
        private final String message;
        private final List<Student> students;

        private QueryResult(boolean success, String message, List<Student> students) {
            this.success = success;
            this.message = message;
            this.students = students;
        }

        public static QueryResult ok(String message, List<Student> students) {
            return new QueryResult(true, message, students);
        }

        public static QueryResult fail(String message, List<Student> students) {
            return new QueryResult(false, message, students);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public List<Student> getStudents() {
            return students;
        }
    }

    public static class StudentFormData {
        private final StudentType type;
        private final String id;
        private final String name;
        private final int age;
        private final String className;
        private final Address address;
        private final Map<String, Double> scores;
        private final String major;
        private final String supervisor;
        private final String direction;

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

