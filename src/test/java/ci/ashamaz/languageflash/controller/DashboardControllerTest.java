package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private UserDetails userDetails;

    @Mock
    private Model model;

    @Mock
    private HttpSession session;

    @InjectMocks
    private DashboardController dashboardController;

    @BeforeEach
    void setUp() {
        // Настройка не требуется, так как используем моки
    }

    @Test
    void testDashboard_Success() {
        // Подготовка данных
        String email = "test@example.com";
        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("user", "testUser");
        dashboardData.put("settings", new HashMap<>());
        dashboardData.put("progressCount", 5);
        dashboardData.put("learnedCount", 10);
        dashboardData.put("customWordsCount", 3);

        when(userDetails.getUsername()).thenReturn(email);
        when(dashboardService.getDashboardData(email)).thenReturn(dashboardData);

        // Вызов тестируемого метода
        String viewName = dashboardController.dashboard(userDetails, model, session);

        // Проверка результатов
        assertEquals("dashboard", viewName);
        verify(dashboardService).getDashboardData(email);
        verify(model).addAllAttributes(dashboardData);
    }

    @Test
    void testDashboard_UserNotFound() {
        // Подготовка данных
        String email = "nonexistent@example.com";
        
        when(userDetails.getUsername()).thenReturn(email);
        when(dashboardService.getDashboardData(email)).thenThrow(new IllegalArgumentException("Пользователь не найден"));

        // Вызов тестируемого метода и проверка исключения
        try {
            dashboardController.dashboard(userDetails, model, session);
            // Если мы дошли до этой точки, тест должен провалиться
            throw new RuntimeException("Ожидалось исключение IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Пользователь не найден", e.getMessage());
        }

        // Проверка, что метод getDashboardData был вызван
        verify(dashboardService).getDashboardData(email);
    }
} 