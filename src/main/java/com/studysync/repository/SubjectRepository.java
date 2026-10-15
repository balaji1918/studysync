package com.studysync.repository;

import com.studysync.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByNameKey(String nameKey);

    /** Inserts the subject if it does not exist yet. Safe when two students add the same subject at once. */
    @Modifying
    @Query(value = "INSERT IGNORE INTO subject (name, name_key) VALUES (:name, :nameKey)", nativeQuery = true)
    void insertIfMissing(@Param("name") String name, @Param("nameKey") String nameKey);
}
