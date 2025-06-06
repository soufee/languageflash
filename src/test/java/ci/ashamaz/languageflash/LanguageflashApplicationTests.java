package ci.ashamaz.languageflash;

import ci.ashamaz.languageflash.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {TestSecurityConfig.class})
class LanguageflashApplicationTests {

    @Test
    void contextLoads() {
    }

}
