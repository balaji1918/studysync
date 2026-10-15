package com.studysync.service;

import com.studysync.entity.Subject;
import com.studysync.repository.SubjectRepository;
import com.studysync.util.TagParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    /** Returns the existing subject with this name (case-insensitive) or creates it. */
    @Transactional
    public Subject findOrCreate(String name) {
        String key = TagParser.key(name);
        subjectRepository.insertIfMissing(name.trim().replaceAll("\\s+", " "), key);
        return subjectRepository.findByNameKey(key)
                .orElseThrow(() -> new IllegalStateException("Subject could not be saved: " + key));
    }
}
