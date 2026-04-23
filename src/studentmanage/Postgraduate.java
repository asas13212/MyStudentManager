package studentmanage;

import java.util.Map;

public class Postgraduate extends Student {
    private static int count = 0;

    private String supervisor;
    private String researchDirection;

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

    public static int getCount() {
        return count;
    }

    public static void decreaseCount() {
        if (count > 0) {
            count--;
        }
    }

    public String getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(String supervisor) {
        this.supervisor = supervisor;
    }

    public String getResearchDirection() {
        return researchDirection;
    }

    public void setResearchDirection(String researchDirection) {
        this.researchDirection = researchDirection;
    }

    @Override
    public StudentType getStudentType() {
        return StudentType.POSTGRADUATE;
    }

    @Override
    protected String getTypeSpecificInfo() {
        return "导师=" + supervisor + "，研究方向=" + researchDirection;
    }
}

