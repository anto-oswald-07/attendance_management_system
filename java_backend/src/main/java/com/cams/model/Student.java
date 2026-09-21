package com.cams.model;

public class Student extends User {

    private String name;
    private String rollNo;
    private String division;

    public Student(int id, String username, String password, String role, String name, String rollNo, String division) {
        super(id, username, password, role);
        this.name = name;
        this.rollNo = rollNo;
        this.division = division;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }
}

