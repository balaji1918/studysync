package com.studysync.entity;

import jakarta.persistence.*;

/** A subject name such as "Java" or "Data Structures". nameKey is the lower-case form used for matching. */
@Entity
@Table(name = "subject")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "name_key", nullable = false, unique = true, length = 100)
    private String nameKey;

    protected Subject() { }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getNameKey() { return nameKey; }
}
