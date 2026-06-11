package ci.ashamaz.languageflash.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
public class SystemSetting {
    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "value", columnDefinition = "text")
    private String value;

    @Column(name = "type", nullable = false)
    private String type = "STRING";

    @Column(columnDefinition = "text")
    private String description;
}
