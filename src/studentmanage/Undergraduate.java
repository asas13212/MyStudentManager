package studentmanage;

import java.util.Map;

public class Undergraduate extends Student {
    private static int count = 0;

    private String major;

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

    public static int getCount() {
        return count;
    }

    public static void decreaseCount() {
        if (count > 0) {
            count--;
        }
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    @Override
    public StudentType getStudentType() {
        return StudentType.UNDERGRADUATE;
    }

    @Override
    protected String getTypeSpecificInfo() {
        return "专业=" + major;
    }
}

