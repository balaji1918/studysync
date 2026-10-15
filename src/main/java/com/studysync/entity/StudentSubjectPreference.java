package com.studysync.entity;

import jakarta.persistence.*;

/** Links a student to a subject they can help with, or want to learn. */
@Entity
@Table(name = "student_subject_preference")
public class StudentSubjectPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "preference_type", nullable = false, length = 20)
    private PreferenceType type;

    protected StudentSubjectPreference() { }

    public StudentSubjectPreference(User user, Subject subject, PreferenceType type) {
        this.user = user;
        this.subject = subject;
        this.type = type;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Subject getSubject() { return subject; }
    public PreferenceType getType() { return type; }
}
