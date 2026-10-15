package com.studysync.service;

import com.studysync.dto.RegistrationForm;
import com.studysync.entity.Role;
import com.studysync.entity.User;
import com.studysync.exception.DuplicateEmailException;
import com.studysync.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    private RegistrationForm validForm() {
        RegistrationForm f = new RegistrationForm();
        f.setFullName("  Asha Rao ");
        f.setEmail("  Asha.Rao@Example.com ");
        f.setCollege("Example College");
        f.setDepartment("AI and Data Science");
        f.setYearOfStudy(2);
        f.setPassword("Secret123");
        f.setConfirmPassword("Secret123");
        return f;
    }

    @Test
    void registersStudentWithHashedPasswordAndNormalizedEmail() {
        when(userRepository.existsByEmailIgnoreCase("asha.rao@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("hashed-value");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        new RegistrationService(userRepository, passwordEncoder).register(validForm());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertEquals("asha.rao@example.com", saved.getEmail());
        assertEquals("hashed-value", saved.getPasswordHash());
        assertNotEquals("Secret123", saved.getPasswordHash());
        assertEquals(Role.STUDENT, saved.getRole());
        assertEquals("Asha Rao", saved.getProfile().getFullName());
        assertSame(saved, saved.getProfile().getUser());
    }

    @Test
    void rejectsDuplicateEmailWithoutSaving() {
        when(userRepository.existsByEmailIgnoreCase("asha.rao@example.com")).thenReturn(true);

        RegistrationService service = new RegistrationService(userRepository, passwordEncoder);

        assertThrows(DuplicateEmailException.class, () -> service.register(validForm()));
        verify(userRepository, never()).saveAndFlush(any());
    }
}
