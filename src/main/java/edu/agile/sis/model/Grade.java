package edu.agile.sis.model;


public class Grade {
    private String subjectId;
    private String subjectName;
   
    private Object gradeValue;
    private Double credits;    

    public Grade(String subjectId, String subjectName, Object gradeValue, Double credits) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.gradeValue = gradeValue;
        this.credits = credits;
    }

    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

  
    public Object getGradeValue() { return gradeValue; }
    public void setGradeValue(Object gradeValue) { this.gradeValue = gradeValue; }

    public Double getCredits() { return credits; }
    public void setCredits(Double credits) { this.credits = credits; }

    @Override
    public String toString() {
        return "Grade{" +
                "subjectId='" + subjectId + '\'' +
                ", subjectName='" + subjectName + '\'' +
                ", gradeValue=" + gradeValue +
                ", credits=" + credits +
                '}';
    }
}
