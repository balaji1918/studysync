package com.studysync.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationFormValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private RegistrationForm valid() {
        RegistrationForm f = new RegistrationForm();
        f.setFullName("Asha Rao");
        f.setEmail("asha@example.com");
        f.setCollege("Example College");
        f.setDepartment("AI and Data Science");
        f.setYearOfStudy(2);
        f.setPassword("Secret123");
        f.setConfirmPassword("Secret123");
        return f;
    }

    @Test
    void validFormHasNoViolations() {
        assertTrue(validator.validate(valid()).isEmpty());
    }

    @Test
    void rejectsBadEmail() {
        RegistrationForm f = valid();
        f.setEmail("not-an-email");
        assertFalse(validator.validateProperty(f, "email").isEmpty());
    }

    @Test
    void rejectsShortPasswordAndPasswordWithoutNumber() {
        RegistrationForm f = valid();
        f.setPassword("abc1");
        assertFalse(validator.validateProperty(f, "password").isEmpty());
        f.setPassword("onlyletters");
        assertFalse(validator.validateProperty(f, "password").isEmpty());
    }

    @Test
    void rejectsMissingFieldsAndInvalidYear() {
        RegistrationForm f = valid();
        f.setFullName(" ");
        f.setYearOfStudy(9);
        assertFalse(validator.validateProperty(f, "fullName").isEmpty());
        assertFalse(validator.validateProperty(f, "yearOfStudy").isEmpty());
    }
}
