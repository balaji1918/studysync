package com.studysync.dto;

import com.studysync.entity.StudyMethod;
import jakarta.validation.constraints.*;

public class ProfileForm {

    @NotBlank(message = "Please enter your full name.")
    @Size(max = 100, message = "Full name can be at most 100 characters.")
    private String fullName;

    @NotBlank(message = "Please enter your college name.")
    @Size(max = 150, message = "College name can be at most 150 characters.")
    private String college;

    @NotBlank(message = "Please enter your department.")
    @Size(max = 100, message = "Department can be at most 100 characters.")
    private String department;

    @NotNull(message = "Please select your year of study.")
    @Min(value = 1, message = "Year of study must be between 1 and 6.")
    @Max(value = 6, message = "Year of study must be between 1 and 6.")
    private Integer yearOfStudy;

    @Size(max = 500, message = "Introduction can be at most 500 characters.")
    private String introduction;

    @Size(max = 500, message = "This list is too long (500 characters maximum).")
    private String canHelpWith;

    @Size(max = 500, message = "This list is too long (500 characters maximum).")
    private String wantToLearn;

    @Size(max = 500, message = "This list is too long (500 characters maximum).")
    private String skills;

    @Size(max = 500, message = "This list is too long (500 characters maximum).")
    private String projectInterests;

    @NotNull(message = "Please choose a preferred study method.")
    private StudyMethod studyMethod = StudyMethod.EITHER;

    @Size(max = 300, message = "Availability can be at most 300 characters.")
    private String availability;

    private boolean discoverable = true;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Integer getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }
    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }
    public String getCanHelpWith() { return canHelpWith; }
    public void setCanHelpWith(String canHelpWith) { this.canHelpWith = canHelpWith; }
    public String getWantToLearn() { return wantToLearn; }
    public void setWantToLearn(String wantToLearn) { this.wantToLearn = wantToLearn; }
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
