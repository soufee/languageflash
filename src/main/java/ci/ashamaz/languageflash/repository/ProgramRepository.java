package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.Program;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    List<Program> findByUserId(Long userId);
}