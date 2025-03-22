package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.Text;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TextRepository extends JpaRepository<Text, Long> {

    @Query("SELECT t FROM Text t WHERE t.language.name = :language AND t.isActive = true AND t.tags LIKE %:tag% ORDER BY t.createdDate DESC")
    Page<Text> findByLanguageAndIsActiveTrueAndTagContains(@Param("language") String language,
                                                           @Param("tag") String tag,
                                                           Pageable pageable);

    @Query("SELECT t FROM Text t WHERE t.language.name = :language AND t.isActive = true ORDER BY t.createdDate DESC")
    Page<Text> findByLanguageAndIsActiveTrueOrderByCreatedDateDesc(@Param("language") String language, Pageable pageable);
}