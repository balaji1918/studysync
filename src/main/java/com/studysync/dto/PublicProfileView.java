package com.studysync.dto;

import java.util.List;

/** What other students are allowed to see. Deliberately has no email field. */
public record PublicProfileView(
        Long userId,
        String fullName,
        String college,
        String department,
        int yearOfStudy,
        String introduction,
        String studyMethodLabel,
        String availability,
        List<String> skills,
        List<String> projectInterests,
        List<String> canHelpWith,
        List<String> wantToLearn,
        boolean discoverable,
        boolean ownProfile) {
}
