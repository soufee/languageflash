package ci.ashamaz.languageflash.service;

import ci.ashamaz.languageflash.model.StudyActivity;
import ci.ashamaz.languageflash.repository.StudyActivityRepository;
import ci.ashamaz.languageflash.repository.UserDictionaryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Статистика для Dashboard (ТЗ 3.6): счётчики, streak, график по дням. */
@Service
public class DashboardService {

    public record DashboardStats(long totalWords, Integer limit, boolean unlimited,
                                 long activeBatch, long learnedTotal,
                                 int learnedToday, int learnedThisWeek,
                                 int streakDays, Map<String, Integer> learnedByDay) {}

    private final UserDictionaryRepository dictionaryRepository;
    private final StudyActivityRepository activityRepository;
    private final DictionaryService dictionaryService;

    public DashboardService(UserDictionaryRepository dictionaryRepository,
                            StudyActivityRepository activityRepository,
                            DictionaryService dictionaryService) {
        this.dictionaryRepository = dictionaryRepository;
        this.activityRepository = activityRepository;
        this.dictionaryService = dictionaryService;
    }

    public DashboardStats stats(Long userId, int days) {
        var dictStatus = dictionaryService.status(userId);
        long active = dictionaryRepository.countByUserIdAndInActiveBatchTrueAndLearnedFalse(userId);
        long learnedTotal = dictionaryRepository.countByUserIdAndLearnedTrue(userId);

        LocalDate today = LocalDate.now();
        int window = Math.min(Math.max(days, 7), 90);
        List<StudyActivity> activities = activityRepository.findSince(userId, today.minusDays(window - 1));

        Map<LocalDate, StudyActivity> byDate = new LinkedHashMap<>();
        activities.forEach(a -> byDate.put(a.getActivityDate(), a));

        int learnedToday = byDate.containsKey(today) ? byDate.get(today).getLearnedCount() : 0;
        int learnedThisWeek = activities.stream()
                .filter(a -> !a.getActivityDate().isBefore(today.minusDays(6)))
                .mapToInt(StudyActivity::getLearnedCount).sum();

        Map<String, Integer> learnedByDay = new LinkedHashMap<>();
        for (int i = window - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            StudyActivity a = byDate.get(date);
            learnedByDay.put(date.toString(), a != null ? a.getLearnedCount() : 0);
        }

        return new DashboardStats(dictStatus.used(), dictStatus.limit(), dictStatus.unlimited(),
                active, learnedTotal, learnedToday, learnedThisWeek,
                streak(userId), learnedByDay);
    }

    /** Серия дней непрерывного обучения: считается по дням с хотя бы одним ответом. */
    private int streak(Long userId) {
        List<LocalDate> dates = activityRepository.findActivityDatesDesc(userId);
        if (dates.isEmpty()) {
            return 0;
        }
        LocalDate expected = LocalDate.now();
        // streak не разрывается, если сегодня ещё не занимался
        if (!dates.get(0).equals(expected)) {
            expected = expected.minusDays(1);
        }
        int streak = 0;
        for (LocalDate date : dates) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (date.isBefore(expected)) {
                break;
            }
        }
        return streak;
    }
}
