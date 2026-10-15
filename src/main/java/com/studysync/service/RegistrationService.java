package com.studysync.service;

import com.studysync.dto.RegistrationForm;
import com.studysync.entity.StudentProfile;
import com.studysync.entity.User;
import com.studysync.exception.DuplicateEmailException;
import com.studysync.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Creates the account and its profile in one transaction. New accounts are always STUDENT. */
    @Transactional
    public User register(RegistrationForm form) {
        String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(form.getPassword()));

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setFullName(form.getFullName().trim());
        profile.setCollege(form.getCollege().trim());
        profile.setDepartment(form.getDepartment().trim());
        profile.setYearOfStudy(form.getYearOfStudy());
        user.setProfile(profile);

        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two people registering the same email at the same moment: the database unique key wins.
            throw new DuplicateEmailException();
        }
    }
}
