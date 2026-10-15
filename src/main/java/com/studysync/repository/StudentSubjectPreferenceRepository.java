package com.studysync.repository;

import com.studysync.entity.PreferenceType;
import com.studysync.entity.StudentSubjectPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface StudentSubjectPreferenceRepository extends JpaRepository<StudentSubjectPreference, Long> {

    @Query("select p from StudentSubjectPreference p join fetch p.subject " +
           "where p.user.id = :userId and p.type = :type order by p.id")
    List<StudentSubjectPreference> findByUserAndType(@Param("userId") Long userId,
                                                     @Param("type") PreferenceType type);

    /** Bulk delete runs immediately, so re-inserting the same subject afterwards is safe. */
    @Modifying
    @Query("delete from StudentSubjectPreference p where p.user.id = :userId and p.type = :type")
    void deleteByUserAndType(@Param("userId") Long userId, @Param("type") PreferenceType type);
}
