package ci.ashamaz.languageflash.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "word_progress")
@Data
public class WordProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "word_id", nullable = false)
    private AbstractWord word;

    @Column(name = "knowledge_factor", nullable = false)
    private float knowledgeFactor = 1.0f;

    @Column(name = "is_learned", nullable = false)
    private boolean isLearned = false;

    @Column(name = "last_reviewed")
    private LocalDateTime lastReviewed;

    @Column(name = "next_review_date")
    private LocalDateTime nextReviewDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private WordSource source = WordSource.PROGRAM;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "text_id")
    private Text text;
}