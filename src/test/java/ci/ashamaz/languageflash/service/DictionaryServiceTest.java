package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.dto.DictionaryDtos.AddWordRequest;
import ci.ashamaz.languageflash.exception.ApiException;
import ci.ashamaz.languageflash.model.*;
import ci.ashamaz.languageflash.repository.UserDictionaryRepository;
import ci.ashamaz.languageflash.repository.UserRepository;
import ci.ashamaz.languageflash.repository.WordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceTest {

    @Mock private UserDictionaryRepository dictionaryRepository;
    @Mock private UserRepository userRepository;
    @Mock private WordRepository wordRepository;
    @Mock private SystemSettingsService settings;

    private DictionaryService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new DictionaryService(dictionaryRepository, userRepository, wordRepository, settings);
        user = new User();
        user.setId(1L);
        user.setEmailConfirmed(true);
        user.setRoles(Set.of("USER"));
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        lenient().when(settings.freeDictionaryLimit()).thenReturn(100);
        lenient().when(dictionaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void freeUserBlockedAtLimit() {
        when(dictionaryRepository.countByUserId(1L)).thenReturn(100L);
        ApiException e = assertThrows(ApiException.class, () -> service.add(1L, manualWord()));
        assertEquals("DICTIONARY_LIMIT_REACHED", e.getCode());
    }

    @Test
    void freeUserCanAddBelowLimit() {
        when(dictionaryRepository.countByUserId(1L)).thenReturn(99L);
        var dto = service.add(1L, manualWord());
        assertEquals("serendipity", dto.word());
        assertEquals(DictionarySource.MANUAL, dto.source());
    }

    @Test
    void premiumUserHasNoLimit() {
        user.setPremium(true);
        user.setPremiumExpiresAt(LocalDateTime.now().plusDays(30));
        var dto = service.add(1L, manualWord());
        assertEquals("serendipity", dto.word());
    }

    @Test
    void expiredPremiumCountsAsFree() {
        user.setPremium(true);
        user.setPremiumExpiresAt(LocalDateTime.now().minusDays(1));
        when(dictionaryRepository.countByUserId(1L)).thenReturn(100L);
        ApiException e = assertThrows(ApiException.class, () -> service.add(1L, manualWord()));
        assertEquals("DICTIONARY_LIMIT_REACHED", e.getCode());
    }

    @Test
    void unconfirmedEmailCannotAddWords() {
        user.setEmailConfirmed(false);
        ApiException e = assertThrows(ApiException.class, () -> service.add(1L, manualWord()));
        assertEquals("EMAIL_NOT_CONFIRMED", e.getCode());
    }

    @Test
    void customWordRequiresWordAndTranslation() {
        when(dictionaryRepository.countByUserId(1L)).thenReturn(0L);
        var request = new AddWordRequest(null, "word", null, null, null, null);
        assertThrows(ApiException.class, () -> service.add(1L, request));
    }

    @Test
    void premiumLevelBlockedForFreeUser() {
        ApiException e = assertThrows(ApiException.class,
                () -> service.checkLevelAccess(user, Level.C1));
        assertEquals("PREMIUM_REQUIRED", e.getCode());
    }

    @Test
    void freeLevelsAccessibleForFreeUser() {
        assertDoesNotThrow(() -> service.checkLevelAccess(user, Level.B2));
    }

    @Test
    void adminBypassesPremiumLevelCheck() {
        user.setRoles(Set.of("ADMIN"));
        assertDoesNotThrow(() -> service.checkLevelAccess(user, Level.C2));
    }

    private AddWordRequest manualWord() {
        return new AddWordRequest(null, "serendipity", "счастливая случайность",
                null, null, DictionarySource.MANUAL);
    }
}
