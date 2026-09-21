package com.cams.model;

public class Faculty extends User {

    private String name;
    private String employeeId;

    public Faculty(int id, String username, String password, String role, String name, String employeeId) {
        super(id, username, password, role);
        this.name = name;
        this.employeeId = employeeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
}

