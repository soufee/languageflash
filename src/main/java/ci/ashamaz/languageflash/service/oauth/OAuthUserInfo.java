package ci.ashamaz.languageflash.service.oauth;

/** Унифицированные данные пользователя от внешнего Identity Provider. */
public record OAuthUserInfo(String providerUserId, String email, String firstName, String lastName) {}
