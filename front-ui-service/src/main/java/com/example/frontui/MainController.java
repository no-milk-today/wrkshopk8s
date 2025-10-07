package com.example.frontui;

import com.example.frontui.service.KeycloakAdminClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
@RequiredArgsConstructor
public class MainController {

    private final FrontUiService frontUiService;
    private final KeycloakAdminClientService keycloakAdminClientService;

    @GetMapping("/")
    public String index() {
        return "redirect:/main";
    }

    @GetMapping("/main")
    public String main(Authentication authentication, Model model) {
        String login;
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            login = oidcUser.getPreferredUsername();
        } else {
            // todo: fallback на пустую строку или прямая ошибка
            login = "";
        }

        log.info("Main page requested for user: {}", login);

        try {
            var mainData = frontUiService.getMainPageData(login);

            // Добавляем все данные в модель для Thymeleaf шаблона main.html
            model.addAttribute("login", mainData.login());
            model.addAttribute("name", mainData.name());
            model.addAttribute("email", mainData.email());
            model.addAttribute("birthdate", mainData.birthdate());
            model.addAttribute("accounts", mainData.accounts());
            model.addAttribute("currency", mainData.currency());
            model.addAttribute("users", mainData.users());
            model.addAttribute("passwordErrors", mainData.passwordErrors());
            model.addAttribute("userAccountsErrors", mainData.userAccountsErrors());
            model.addAttribute("cashErrors", mainData.cashErrors());
            model.addAttribute("transferErrors", mainData.transferErrors());
            model.addAttribute("transferOtherErrors", mainData.transferOtherErrors());

            return "main";
        } catch (Exception e) {
            log.error("Error fetching main page data for user: {}", login, e);
            model.addAttribute("error", "Ошибка при загрузке данных пользователя: " + e.getMessage());
            model.addAttribute("login", login);
            return "main";
        }
    }

    @PostMapping("/user/{login}/editPassword")
    public String editPassword(
            @PathVariable("login") String login,
            @RequestParam("password") String password,
            @RequestParam("confirm_password") String confirmPassword,  // здесь имя из формы
            Model model
    ) {
        List<String> errors = frontUiService.changePassword(login, password, confirmPassword);
        return redirectToMainWithErrors(login, model, "passwordErrors", errors);
    }

    private String redirectToMainWithErrors(
            String login, Model model, String field, List<String> errors
    ) {
        // Получаем свежие данные для main
        var mainData = frontUiService.getMainPageData(login);
        model.addAllAttributes(Map.of(
                "login", mainData.login(),
                "name", mainData.name(),
                "email", mainData.email(),
                "birthdate", mainData.birthdate(),
                "accounts", mainData.accounts(),
                "currency", mainData.currency(),
                "users", mainData.users(),
                field, errors
        ));
        return "main";
    }

    @GetMapping("/signup")
    public String signup() {
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(
            @RequestParam("login") String login,
            @RequestParam("password") String password,
            @RequestParam("confirm_password") String confirmPassword,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("birthdate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthdate,
            Model model
    ) {
        log.info("Registration request for user: {}", login);

        if (!password.equals(confirmPassword)) {
            model.addAttribute("errors", List.of("Пароли не совпадают"));
            return "signup";
        }

        /* Keycloak */
        try {
            keycloakAdminClientService.createUser(login, password, email, name).block();
        } catch (Exception ex) {
            model.addAttribute("errors", List.of("Keycloak: " + ex.getMessage()));
            return "signup";
        }

        /* Customer profile */
        var resp = frontUiService.registerCustomer(login, null, null, name, email, birthdate);
        if (!resp.success()) {
            model.addAttribute("errors", resp.errors());
            return "signup";
        }

        // redirect to OAuth2-login flow
        return "redirect:/oauth2/authorization/keycloak";
    }

    @PostMapping("/user/{login}/editUserAccounts")
    public String editUserAccounts(
            @PathVariable("login") String login,
            @RequestParam("name") String name,
            @RequestParam("birthdate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthdate,
            @RequestParam(value = "account", required = false) List<String> accounts,
            Model model
    ) {
        log.info("Edit user accounts request for user: {}", login);

        // Если accounts null, передаем пустой список
        if (accounts == null) {
            accounts = List.of();
        }

        List<String> errors = frontUiService.editUserAccounts(login, name, birthdate, accounts);
        return redirectToMainWithErrors(login, model, "userAccountsErrors", errors);
    }

    @PostMapping("/user/{login}/cash")
    public String processCash(
            @PathVariable("login") String login,
            @RequestParam("currency") String currency,
            @RequestParam("value") BigDecimal value,
            @RequestParam("action") String action,
            Model model) {

        log.info("Cash operation request for user {}: {} {} {}", login, action, value, currency);

        List<String> errors = frontUiService.processCashOperation(login, currency, value, action);

        if (!errors.isEmpty()) {
            return redirectToMainWithErrors(login, model, "cashErrors", errors);
        }

        return "redirect:/main";
    }

    @PostMapping("/user/{login}/transfer")
    public String processTransfer(
            @PathVariable("login") String login,
            @RequestParam("from_currency") String fromCurrency,
            @RequestParam("to_currency") String toCurrency,
            @RequestParam("value") BigDecimal value,
            @RequestParam("to_login") String toLogin,
            Model model) {

        log.info("Transfer request for user {}: {} {} -> {} {}, to user: {}",
                 login, value, fromCurrency, value, toCurrency, toLogin);

        List<String> errors = frontUiService.processTransferOperation(login, fromCurrency, toCurrency, value, toLogin);
        if (!errors.isEmpty()) {
            // если перевод самому себе - transferErrors, иначе transferOtherErrors
            var errorField = login.equals(toLogin) ? "transferErrors" : "transferOtherErrors";
            return redirectToMainWithErrors(login, model, errorField, errors);
        }

        return "redirect:/main";
    }
}
