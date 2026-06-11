package ci.ashamaz.languageflash.controller;

import ci.ashamaz.languageflash.dto.DictionaryDtos.EntryDto;
import ci.ashamaz.languageflash.security.UserPrincipal;
import ci.ashamaz.languageflash.service.SyncService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Синхронизация офлайн-прогресса — заложено для мобильных клиентов (ТЗ 3.9). */
@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    public record ProgressSyncRequest(@NotNull List<SyncService.OfflineEvent> events) {}

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @PostMapping("/progress")
    public SyncService.SyncResult progress(@AuthenticationPrincipal UserPrincipal principal,
                                           @Valid @RequestBody ProgressSyncRequest request) {
        return syncService.applyProgress(principal.id(), request.events());
    }

    @GetMapping("/dictionary-snapshot")
    public List<EntryDto> dictionarySnapshot(@AuthenticationPrincipal UserPrincipal principal,
                                             @RequestParam Long languageId,
                                             @RequestParam List<String> levels) {
        return syncService.dictionarySnapshot(principal.id(), languageId, levels);
    }
}
