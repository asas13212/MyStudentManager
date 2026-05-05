package studentmanage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 作者：cyt
 * 功能：学生业务服务，封装核心业务逻辑。
 * 编写时间：2026-05-05
 */
public class StudentService {
    /** 学生仓储。作者：cyt；编写时间：2026-05-05 */
    private final StudentRepository repository;

    /**
     * 作者：cyt
     * 功能：创建业务服务。
     * 编写时间：2026-05-05
     * @param repository 学生仓储
     */
    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    /**
     * 作者：cyt
     * 功能：新增学生。
     * 编写时间：2026-05-05
     * @param student 学生对象
     * @return 是否新增成功
     */
    public boolean addStudent(Student student) {
        // 学号全局唯一：已存在则拒绝新增。
        if (repository.findById(student.getStudentId()).isPresent()) {
            return false;
        }
        return repository.add(student);
    }

    /**
     * 作者：cyt
     * 功能：更新学生基本信息。
     * 编写时间：2026-05-05
     * @param studentId 学号
     * @param name 姓名
     * @param age 年龄
     * @param className 班级
     * @param address 地址
     * @param scores 成绩
     * @param major 专业
     * @param supervisor 导师
     * @param researchDirection 研究方向
     * @return 是否更新成功
     */
    public boolean updateStudentBasicInfo(
            String studentId,
            String name,
            int age,
            String className,
            Address address,
            Map<String, Double> scores,
            String major,
            String supervisor,
            String researchDirection
    ) {
        Optional<Student> optional = repository.findById(studentId);
        if (optional.isEmpty()) {
            return false;
        }

        Student student = optional.get();
        // 先更新所有学生共有字段。
        student.setName(name);
        student.setAge(age);
        student.setClassName(className);
        student.setAddress(address);
        student.replaceAllScores(scores);

        // 再按具体类型更新扩展字段，体现多态扩展点。
        if (student instanceof Undergraduate && major != null) {
            ((Undergraduate) student).setMajor(major);
        }
        if (student instanceof Postgraduate) {
            if (supervisor != null) {
                ((Postgraduate) student).setSupervisor(supervisor);
            }
            if (researchDirection != null) {
                ((Postgraduate) student).setResearchDirection(researchDirection);
            }
        }
        return true;
    }

    /**
     * 作者：cyt
     * 功能：按学号删除学生。
     * 编写时间：2026-05-05
     * @param studentId 学号
     * @return 是否删除成功
     */
    public boolean deleteStudentById(String studentId) {
        return repository.removeById(studentId);
    }

    /**
     * 作者：cyt
     * 功能：清空所有学生。
     * 编写时间：2026-05-05
     */
    public void clearAllStudents() {
        repository.clearAll();
    }

    /**
     * 作者：cyt
     * 功能：按学号查找学生。
     * 编写时间：2026-05-05
     * @param studentId 学号
     * @return 学生Optional
     */
    public Optional<Student> findById(String studentId) {
        return repository.findById(studentId);
    }

    /**
     * 作者：cyt
     * 功能：按姓名模糊查找学生。
     * 编写时间：2026-05-05
     * @param name 姓名
     * @return 学生列表
     */
    public List<Student> findByName(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        return repository.getAll()
                .stream()
                .filter(s -> s.getName().toLowerCase(Locale.ROOT).contains(key))
                .collect(Collectors.toList());
    }

    /**
     * 作者：cyt
     * 功能：按班级查找学生。
     * 编写时间：2026-05-05
     * @param className 班级
     * @return 学生列表
     */
    public List<Student> findByClassName(String className) {
        return repository.getAll()
                .stream()
                .filter(s -> s.getClassName().equalsIgnoreCase(className))
                .collect(Collectors.toList());
    }

    /**
     * 作者：cyt
     * 功能：列出所有学生。
     * 编写时间：2026-05-05
     * @return 学生列表
     */
    public List<Student> listAll() {
        return repository.getAll();
    }

    /**
     * 作者：cyt
     * 功能：按类型列出学生。
     * 编写时间：2026-05-05
     * @param type 学生类型
     * @return 学生列表
     */
    public List<Student> listByType(StudentType type) {
        return repository.getAll().stream().filter(s -> s.getStudentType() == type).collect(Collectors.toList());
    }

    /**
     * 作者：cyt
     * 功能：对学生列表进行排序。
     * 编写时间：2026-05-05
     * @param source 源列表
     * @param sortField 排序字段
     * @param subjectName 单科名称
     * @param ascending 是否升序
     * @return 排序结果
     */
    public List<Student> sortStudents(List<Student> source, SortField sortField, String subjectName, boolean ascending) {
        List<Student> result = new ArrayList<>(source);
        Comparator<Student> comparator;

        switch (sortField) {
            case TOTAL_SCORE:
                comparator = Comparator.comparingDouble(Student::getTotalScore);
                break;
            case SUBJECT_SCORE:
                comparator = Comparator.comparingDouble(s -> s.getSubjectScore(subjectName));
                break;
            case STUDENT_ID:
            default:
                comparator = Comparator.comparing(Student::getStudentId);
                break;
        }

        if (!ascending) {
            comparator = comparator.reversed();
        }

        // 使用学号作为二级排序键，避免分数相同导致输出顺序不稳定。
        result.sort(comparator.thenComparing(Student::getStudentId));
        return result;
    }

    /**
     * 作者：cyt
     * 功能：获取本科生人数。
     * 编写时间：2026-05-05
     * @return 本科生人数
     */
    public int getUndergraduateCount() {
        return Undergraduate.getCount();
    }

    /**
     * 作者：cyt
     * 功能：获取研究生人数。
     * 编写时间：2026-05-05
     * @return 研究生人数
     */
    public int getPostgraduateCount() {
        return Postgraduate.getCount();
    }

    /**
     * 作者：cyt
     * 功能：获取学生总人数。
     * 编写时间：2026-05-05
     * @return 学生总人数
     */
    public int getTotalCount() {
        return Student.getTotalCount();
    }

    /**
     * 作者：cyt
     * 功能：判断仓储是否已满。
     * 编写时间：2026-05-05
     * @return 是否已满
     */
    public boolean isFull() {
        return repository.isFull();
    }

    /**
     * 作者：cyt
     * 功能：判断仓储是否为空。
     * 编写时间：2026-05-05
     * @return 是否为空
     */
    public boolean isEmpty() {
        return repository.isEmpty();
    }

    /**
     * 作者：cyt
     * 功能：获取当前学生数量。
     * 编写时间：2026-05-05
     * @return 当前数量
     */
    public int getCurrentSize() {
        return repository.size();
    }

    /**
     * 作者：cyt
     * 功能：获取仓储容量。
     * 编写时间：2026-05-05
     * @return 容量
     */
    public int getCapacity() {
        return repository.getCapacity();
    }
}
