package com.studysync.dto;

import jakarta.validation.constraints.*;

public class RegistrationForm {

    @NotBlank(message = "Please enter your full name.")
    @Size(max = 100, message = "Full name can be at most 100 characters.")
    private String fullName;

    @NotBlank(message = "Please enter your email address.")
    @Email(message = "Please enter a valid email address.")
    @Size(max = 254, message = "Email can be at most 254 characters.")
    private String email;

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

    // BCrypt only uses the first 72 bytes, so we cap the length at 72.
    @NotBlank(message = "Please choose a password.")
    @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters long.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "Password must contain at least one letter and one number.")
    private String password;

    @NotBlank(message = "Please confirm your password.")
    private String confirmPassword;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public Integer getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(Integer yearOfStudy) { this.yearOfStudy = yearOfStudy; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
