package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.Word;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WordRepository extends JpaRepository<Word, Long> {
    List<Word> findByLanguageId(Long languageId);
}
