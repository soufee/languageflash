package ci.ashamaz.languageflash.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class LearnControllerTest {

    @InjectMocks
    private LearnController learnController;

    @Test
    void testLearn() {
        // Вызов тестируемого метода
        String viewName = learnController.learn();

        // Проверка результатов
        assertEquals("learn", viewName);
    }
} 