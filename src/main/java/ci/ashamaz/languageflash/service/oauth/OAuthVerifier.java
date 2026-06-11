package ci.ashamaz.languageflash.service.oauth;

/**
 * Контракт верификации identity-токена внешнего провайдера (Google/Apple/VK).
 * Реализации проверяют подпись/валидность токена на стороне провайдера.
 */
public interface OAuthVerifier {
    String provider();
    boolean isConfigured();
    OAuthUserInfo verify(String identityToken);
}
