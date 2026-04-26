package studentmanage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class StudentService {
    private final StudentRepository repository;

    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    public boolean addStudent(Student student) {
        // 学号全局唯一：已存在则拒绝新增。
        if (repository.findById(student.getStudentId()).isPresent()) {
            return false;
        }
        return repository.add(student);
    }

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

    public boolean deleteStudentById(String studentId) {
        return repository.removeById(studentId);
    }

    public void clearAllStudents() {
        repository.clearAll();
    }

    public Optional<Student> findById(String studentId) {
        return repository.findById(studentId);
    }

    public List<Student> findByName(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        return repository.getAll()
                .stream()
                .filter(s -> s.getName().toLowerCase(Locale.ROOT).contains(key))
                .collect(Collectors.toList());
    }

    public List<Student> findByClassName(String className) {
        return repository.getAll()
                .stream()
                .filter(s -> s.getClassName().equalsIgnoreCase(className))
                .collect(Collectors.toList());
    }

    public List<Student> listAll() {
        return repository.getAll();
    }

    public List<Student> listByType(StudentType type) {
        return repository.getAll().stream().filter(s -> s.getStudentType() == type).collect(Collectors.toList());
    }

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

    public int getUndergraduateCount() {
        return Undergraduate.getCount();
    }

    public int getPostgraduateCount() {
        return Postgraduate.getCount();
    }

    public int getTotalCount() {
        return Student.getTotalCount();
    }

    public boolean isFull() {
        return repository.isFull();
    }

    public boolean isEmpty() {
        return repository.isEmpty();
    }

    public int getCurrentSize() {
        return repository.size();
    }

    public int getCapacity() {
        return repository.getCapacity();
    }
}

