package com.studysync.service;

import com.studysync.dto.ProfileForm;
import com.studysync.dto.PublicProfileView;
import com.studysync.entity.*;
import com.studysync.exception.InvalidProfileInputException;
import com.studysync.exception.NotFoundException;
import com.studysync.repository.StudentProfileRepository;
import com.studysync.repository.StudentSubjectPreferenceRepository;
import com.studysync.repository.UserRepository;
import com.studysync.util.TagParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfileService {

    private static final int MAX_ITEMS = 15;
    private static final int MAX_SUBJECT_LENGTH = 60;
    private static final int MAX_TAG_LENGTH = 40;

    private final StudentProfileRepository profileRepository;
    private final StudentSubjectPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final SubjectService subjectService;

    public ProfileService(StudentProfileRepository profileRepository,
                          StudentSubjectPreferenceRepository preferenceRepository,
                          UserRepository userRepository,
                          SubjectService subjectService) {
        this.profileRepository = profileRepository;
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
        this.subjectService = subjectService;
    }

    /** Fills the edit form with the logged-in student's saved data. */
    @Transactional(readOnly = true)
    public ProfileForm loadForm(Long userId) {
        StudentProfile p = findProfile(userId);
        ProfileForm form = new ProfileForm();
        form.setFullName(p.getFullName());
        form.setCollege(p.getCollege());
        form.setDepartment(p.getDepartment());
        form.setYearOfStudy(p.getYearOfStudy());
        form.setIntroduction(p.getIntroduction());
        form.setSkills(p.getSkills());
        form.setProjectInterests(p.getProjectInterests());
        form.setStudyMethod(p.getStudyMethod());
        form.setAvailability(p.getAvailability());
        form.setDiscoverable(p.isDiscoverable());
        form.setCanHelpWith(String.join(", ", subjectNames(userId, PreferenceType.CAN_HELP)));
        form.setWantToLearn(String.join(", ", subjectNames(userId, PreferenceType.WANT_TO_LEARN)));
        return form;
    }

    /**
     * Saves the logged-in student's own profile. The user id comes from the login session,
     * never from the form, so nobody can edit another student's profile.
     */
    @Transactional
    public void updateProfile(Long userId, ProfileForm form) {
        // Check every list first so we never save half of a profile.
        List<String> canHelp = parseList("canHelpWith", form.getCanHelpWith(), MAX_SUBJECT_LENGTH);
        List<String> wantToLearn = parseList("wantToLearn", form.getWantToLearn(), MAX_SUBJECT_LENGTH);
        List<String> skills = parseList("skills", form.getSkills(), MAX_TAG_LENGTH);
        List<String> interests = parseList("projectInterests", form.getProjectInterests(), MAX_TAG_LENGTH);

        StudentProfile p = findProfile(userId);
        p.setFullName(form.getFullName().trim());
        p.setCollege(form.getCollege().trim());
        p.setDepartment(form.getDepartment().trim());
        p.setYearOfStudy(form.getYearOfStudy());
        p.setIntroduction(blankToNull(form.getIntroduction()));
        p.setSkills(skills.isEmpty() ? null : String.join(", ", skills));
        p.setProjectInterests(interests.isEmpty() ? null : String.join(", ", interests));
        p.setStudyMethod(form.getStudyMethod());
        p.setAvailability(blankToNull(form.getAvailability()));
        p.setDiscoverable(form.isDiscoverable());
        profileRepository.save(p);

        replacePreferences(userId, PreferenceType.CAN_HELP, canHelp);
        replacePreferences(userId, PreferenceType.WANT_TO_LEARN, wantToLearn);
    }

    /**
     * A profile can be seen by its owner, or by other students if the owner allows discovery
     * and the account is active. Otherwise we answer "not found" so private profiles are not revealed.
     */
    @Transactional(readOnly = true)
    public PublicProfileView viewProfile(Long targetUserId, Long viewerUserId) {
        StudentProfile p = profileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
        boolean own = targetUserId.equals(viewerUserId);
        if (!own && (!p.isDiscoverable() || p.getUser().getStatus() != AccountStatus.ACTIVE)) {
            throw new NotFoundException("Profile not found");
        }
        return new PublicProfileView(
                targetUserId,
                p.getFullName(),
                p.getCollege(),
                p.getDepartment(),
                p.getYearOfStudy(),
                p.getIntroduction(),
                p.getStudyMethod().getLabel(),
                p.getAvailability(),
                TagParser.parse(p.getSkills(), 100, 500),
                TagParser.parse(p.getProjectInterests(), 100, 500),
                subjectNames(targetUserId, PreferenceType.CAN_HELP),
                subjectNames(targetUserId, PreferenceType.WANT_TO_LEARN),
                p.isDiscoverable(),
                own);
    }

    // ----- helpers -----

    private StudentProfile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Profile not found"));
    }

    private List<String> subjectNames(Long userId, PreferenceType type) {
        return preferenceRepository.findByUserAndType(userId, type).stream()
                .map(pref -> pref.getSubject().getName())
                .toList();
    }

    private void replacePreferences(Long userId, PreferenceType type, List<String> names) {
        preferenceRepository.deleteByUserAndType(userId, type);
        User userRef = userRepository.getReferenceById(userId);
        for (String name : names) {
            Subject subject = subjectService.findOrCreate(name);
            preferenceRepository.save(new StudentSubjectPreference(userRef, subject, type));
        }
    }

    private List<String> parseList(String field, String raw, int maxLength) {
        try {
            return TagParser.parse(raw, MAX_ITEMS, maxLength);
        } catch (IllegalArgumentException e) {
            throw new InvalidProfileInputException(field, e.getMessage());
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
