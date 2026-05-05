package studentmanage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 作者：cyt
 * 功能：学生仓储，负责基础存储与查找。
 * 编写时间：2026-05-05
 */
public class StudentRepository {
    /** 最大容量。作者：cyt；编写时间：2026-05-05 */
    private final int capacity;
    /** 学生列表。作者：cyt；编写时间：2026-05-05 */
    private final List<Student> students;

    /**
     * 作者：cyt
     * 功能：创建仓储并设置容量。
     * 编写时间：2026-05-05
     * @param capacity 容量
     */
    public StudentRepository(int capacity) {
        this.capacity = capacity;
        this.students = new ArrayList<>();
    }

    /**
     * 作者：cyt
     * 功能：获取仓储容量。
     * 编写时间：2026-05-05
     * @return 容量
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * 作者：cyt
     * 功能：获取当前大小。
     * 编写时间：2026-05-05
     * @return 学生数量
     */
    public int size() {
        return students.size();
    }

    /**
     * 作者：cyt
     * 功能：判断是否已满。
     * 编写时间：2026-05-05
     * @return 是否已满
     */
    public boolean isFull() {
        return students.size() >= capacity;
    }

    /**
     * 作者：cyt
     * 功能：判断是否为空。
     * 编写时间：2026-05-05
     * @return 是否为空
     */
    public boolean isEmpty() {
        return students.isEmpty();
    }

    /**
     * 作者：cyt
     * 功能：添加学生。
     * 编写时间：2026-05-05
     * @param student 学生
     * @return 是否添加成功
     */
    public boolean add(Student student) {
        // 边界条件：容量已满时拒绝新增。
        if (isFull()) {
            return false;
        }
        students.add(student);
        return true;
    }

    /**
     * 作者：cyt
     * 功能：按学号查找学生。
     * 编写时间：2026-05-05
     * @param studentId 学号
     * @return 学生Optional
     */
    public Optional<Student> findById(String studentId) {
        return students.stream().filter(s -> s.getStudentId().equals(studentId)).findFirst();
    }

    /**
     * 作者：cyt
     * 功能：按学号删除学生。
     * 编写时间：2026-05-05
     * @param studentId 学号
     * @return 是否删除成功
     */
    public boolean removeById(String studentId) {
        // 边界条件：空仓储直接返回，避免无意义查找。
        if (isEmpty()) {
            return false;
        }
        Optional<Student> target = findById(studentId);
        if (target.isEmpty()) {
            return false;
        }

        Student student = target.get();
        students.remove(student);
        // 删除成功后同步维护静态计数，保证统计结果准确。
        Student.decreaseTotalCount();
        if (student instanceof Undergraduate) {
            Undergraduate.decreaseCount();
        } else if (student instanceof Postgraduate) {
            Postgraduate.decreaseCount();
        }
        return true;
    }

    /**
     * 作者：cyt
     * 功能：获取全部学生列表副本。
     * 编写时间：2026-05-05
     * @return 学生列表
     */
    public List<Student> getAll() {
        return new ArrayList<>(students);
    }

    /**
     * 作者：cyt
     * 功能：清空所有学生数据。
     * 编写时间：2026-05-05
     */
    public void clearAll() {
        for (Student student : students) {
            Student.decreaseTotalCount();
            if (student instanceof Undergraduate) {
                Undergraduate.decreaseCount();
            } else if (student instanceof Postgraduate) {
                Postgraduate.decreaseCount();
            }
        }
        students.clear();
    }
}
