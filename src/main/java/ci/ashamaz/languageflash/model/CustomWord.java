package ci.ashamaz.languageflash.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@DiscriminatorValue("CUSTOM")
@Getter
@Setter
public class CustomWord extends AbstractWord {
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}