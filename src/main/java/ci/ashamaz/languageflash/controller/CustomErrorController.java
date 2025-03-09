package ci.ashamaz.languageflash.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error/404"; // Страница 404 для несуществующих путей
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return "error/403"; // Страница 403 для доступа без авторизации
            }
        }
        // Для всех остальных ошибок (включая null status)
        model.addAttribute("error", "Произошла непредвиденная ошибка");
        return "error/general"; // Общая страница ошибки
    }
}