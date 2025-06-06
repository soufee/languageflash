package ci.ashamaz.languageflash.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/learn")
@Slf4j
public class LearnController {

    @GetMapping
    public String learn() {
        log.info("Handling GET /learn");
        return "learn";
    }
}