package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.Dictionary;
import ci.ashamaz.languageflash.model.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface DictionaryRepository extends JpaRepository<Dictionary, Long> {
    List<Dictionary> findByLanguageLevelId(Long languageLevelId);

    @Query("SELECT d.id, COUNT(w) FROM Dictionary d LEFT JOIN d.words w GROUP BY d.id")
    Map<Long, Long> countWordsByDictionary();

    @Query("SELECT COUNT(w) FROM Dictionary d LEFT JOIN d.words w WHERE d.id = :dictionaryId")
    Long countWordsByDictionaryId(@Param("dictionaryId") Long dictionaryId);

    // Новый метод для пагинации слов
    @Query("SELECT w FROM Word w JOIN w.dictionaries d WHERE d.id = :dictionaryId")
    Page<Word> findWordsByDictionaryId(@Param("dictionaryId") Long dictionaryId, Pageable pageable);
}