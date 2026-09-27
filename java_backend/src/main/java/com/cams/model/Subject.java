package com.cams.model;

public class Subject {

    private int id;
    private String name;
    private String code;
    private String division;

    public Subject() {
    }

    public Subject(int id, String name, String code, String division) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.division = division;
    }

    public Subject(String name, String code, String division) {
        this.name = name;
        this.code = code;
        this.division = division;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }
}
