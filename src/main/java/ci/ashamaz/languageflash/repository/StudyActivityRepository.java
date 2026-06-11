package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.StudyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudyActivityRepository extends JpaRepository<StudyActivity, Long> {
    Optional<StudyActivity> findByUserIdAndActivityDate(Long userId, LocalDate date);

    @Query("SELECT s FROM StudyActivity s WHERE s.user.id = :userId AND s.activityDate >= :from ORDER BY s.activityDate ASC")
    List<StudyActivity> findSince(@Param("userId") Long userId, @Param("from") LocalDate from);

    @Query("SELECT s.activityDate FROM StudyActivity s WHERE s.user.id = :userId AND s.answersCount > 0 ORDER BY s.activityDate DESC")
    List<LocalDate> findActivityDatesDesc(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}
