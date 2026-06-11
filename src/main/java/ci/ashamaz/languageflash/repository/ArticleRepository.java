package ci.ashamaz.languageflash.repository;

import ci.ashamaz.languageflash.model.Article;
import ci.ashamaz.languageflash.model.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    @Query("""
            SELECT a FROM Article a
            WHERE a.status = :status
              AND (a.owner IS NULL OR a.owner.id = :userId)
              AND (:languageId IS NULL OR a.language.id = :languageId)
              AND (:levelOrder IS NULL OR a.levelOrder = :levelOrder)
              AND (:tag IS NULL OR a.tags LIKE CONCAT('%"', :tag, '"%'))
            ORDER BY a.createdAt DESC
            """)
    Page<Article> findFiltered(@Param("status") ArticleStatus status,
                               @Param("userId") Long userId,
                               @Param("languageId") Long languageId,
                               @Param("levelOrder") Integer levelOrder,
                               @Param("tag") String tag,
                               Pageable pageable);

    Page<Article> findByOwnerIsNullOrderByCreatedAtDesc(Pageable pageable);
}
