package com.studysync.entity;

public enum StudyMethod {
    ONLINE("Online"), IN_PERSON("In person"), EITHER("Online or in person");

    private final String label;
    StudyMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}
