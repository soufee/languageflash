package ci.ashamaz.languageflash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LanguageflashApplication {

    public static void main(String[] args) {
        SpringApplication.run(LanguageflashApplication.class, args);
    }
}
