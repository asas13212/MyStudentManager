package studentmanage;

public enum StudentType {
    UNDERGRADUATE("本科生"),
    POSTGRADUATE("研究生");

    private final String displayName;

    StudentType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

