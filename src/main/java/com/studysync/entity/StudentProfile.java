package com.studysync.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** Public-facing student details (never contains email or password). */
@Entity
@Table(name = "student_profile")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String college;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(name = "year_of_study", nullable = false)
    private int yearOfStudy;

    @Column(length = 500)
    private String introduction;

    /** Comma separated, e.g. "Java, SQL, HTML". */
    @Column(length = 500)
    private String skills;

    @Column(name = "project_interests", length = 500)
    private String projectInterests;

    @Enumerated(EnumType.STRING)
    @Column(name = "study_method", nullable = false, length = 20)
    private StudyMethod studyMethod = StudyMethod.EITHER;

    @Column(length = 300)
    private String availability;

    /** If false, other students cannot see this profile or find it in discovery pages. */
    @Column(nullable = false)
    private boolean discoverable = true;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }
    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getProjectInterests() { return projectInterests; }
    public void setProjectInterests(String projectInterests) { this.projectInterests = projectInterests; }
    public StudyMethod getStudyMethod() { return studyMethod; }
    public void setStudyMethod(StudyMethod studyMethod) { this.studyMethod = studyMethod; }
    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }
    public boolean isDiscoverable() { return discoverable; }
    public void setDiscoverable(boolean discoverable) { this.discoverable = discoverable; }
}
