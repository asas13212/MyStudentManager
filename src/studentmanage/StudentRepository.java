package studentmanage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentRepository {
    private final int capacity;
    private final List<Student> students;

    public StudentRepository(int capacity) {
        this.capacity = capacity;
        this.students = new ArrayList<>();
    }

    public int getCapacity() {
        return capacity;
    }

    public int size() {
        return students.size();
    }

    public boolean isFull() {
        return students.size() >= capacity;
    }

    public boolean isEmpty() {
        return students.isEmpty();
    }

    public boolean add(Student student) {
        // 边界条件：容量已满时拒绝新增。
        if (isFull()) {
            return false;
        }
        students.add(student);
        return true;
    }

    public Optional<Student> findById(String studentId) {
        return students.stream().filter(s -> s.getStudentId().equals(studentId)).findFirst();
    }

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

    public List<Student> getAll() {
        return new ArrayList<>(students);
    }
}

