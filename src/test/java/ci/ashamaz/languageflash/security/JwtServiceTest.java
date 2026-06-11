package ci.ashamaz.languageflash.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-0123456789abcdef0123456789abcdef";

    @Test
    void generatesAndParsesToken() {
        JwtService service = new JwtService(SECRET, 60_000);
        String token = service.generateAccessToken(42L, "user@example.com", Set.of("USER", "ADMIN"));

        Claims claims = service.parse(token);
        assertNotNull(claims);
        assertEquals("user@example.com", claims.getSubject());
        assertEquals(42L, claims.get("uid", Number.class).longValue());
        assertTrue(claims.get("roles", List.class).contains("ADMIN"));
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtService service = new JwtService(SECRET, 1);
        String token = service.generateAccessToken(1L, "user@example.com", Set.of("USER"));
        Thread.sleep(20);
        assertNull(service.parse(token));
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        JwtService a = new JwtService(SECRET, 60_000);
        JwtService b = new JwtService("another-secret-key-0123456789abcdef012345678", 60_000);
        String token = b.generateAccessToken(1L, "user@example.com", Set.of("USER"));
        assertNull(a.parse(token));
    }

    @Test
    void rejectsGarbage() {
        JwtService service = new JwtService(SECRET, 60_000);
        assertNull(service.parse("garbage.token.here"));
    }
}
