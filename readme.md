# Language Flash («Зубрилка»)

Платформа для изучения иностранных языков: личный словарь, интервальное повторение,
режим 25-го кадра (RSVP), интерактивный разбор текстов с переводом по клику,
Freemium-монетизация (Premium-подписка + реклама).

## Архитектура

- **Backend** — Java 17, Spring Boot 3.4, Spring Security 6 (stateless JWT + refresh-ротация),
  Spring Data JPA, PostgreSQL, Liquibase. Headless REST API (`/api/v1/**`) — единый бэкенд
  для веба и будущих мобильных клиентов (эндпоинты `/api/v1/sync/**` для офлайн-синхронизации
  уже заложены).
- **Frontend (Web)** — React 18 + TypeScript + Vite, SPA с тёмной glassmorphism-темой.
- **Mobile** — не реализовано; архитектура (общий API, verify-purchase, sync) готова к подключению Flutter-клиента.

## Быстрый старт (локальная разработка)

Требования: Java 17, Maven, Node 18+, PostgreSQL (база `wordflash`, пользователь `postgres/postgres`).

```bash
# 1. Бэкенд (порт 8087, миграции Liquibase применятся автоматически)
mvn spring-boot:run

# 2. Фронтенд (порт 5173, проксирует /api на 8087)
cd frontend
npm install
npm run dev
```

Откройте http://localhost:5173.

В режиме разработки письма (код подтверждения email, сброс пароля) **пишутся в лог бэкенда**
(`app.mail.mock=true`) — ищите `[MOCK EMAIL]`.

### Запуск через Docker Compose

```bash
docker compose up --build
# веб: http://localhost:3000, API: http://localhost:8087
```

## Конфигурация (переменные окружения)

| Переменная | Назначение | По умолчанию |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC-URL базы | `jdbc:postgresql://localhost:5432/wordflash` |
| `JWT_SECRET` | Секрет подписи JWT (обязателен в проде) | dev-значение |
| `APP_MAIL_MOCK` | Письма в лог вместо SMTP | `true` |
| `SPRING_MAIL_USERNAME/PASSWORD` | SMTP-креды (Yandex) | пусто |
| `OAUTH_GOOGLE_CLIENT_ID` | Google OAuth (пусто = выключен) | пусто |
| `OAUTH_APPLE_CLIENT_ID` | Sign in with Apple | пусто |
| `OAUTH_VK_CLIENT_ID` / `OAUTH_VK_SERVICE_TOKEN` | VK ID | пусто |
| `TRANSLATION_PROVIDER` | `MYMEMORY` (без ключа) / `YANDEX` | `MYMEMORY` |
| `YANDEX_TRANSLATE_API_KEY` | Ключ Яндекс.Переводчика | пусто |
| `BILLING_PROVIDER` | `MOCK` (эмуляция оплаты) / боевой шлюз | `MOCK` |
| `RATE_LIMIT_ENABLED` | Rate limiting | `true` |

## Что реализовано (по ТЗ)

- **Авторизация**: регистрация с подтверждением email, вход, refresh-токены с ротацией,
  восстановление пароля, OAuth2-эндпоинт (Google/Apple/VK — активируются ключами),
  удаление аккаунта (GDPR).
- **Личный словарь**: системные + пользовательские слова, источник (SYSTEM/ARTICLE/MANUAL),
  лимит 100 слов для Free (настраивается в админке), безлимит для Premium.
- **Обучение**: flashcards с алгоритмом интервального повторения
  (×0.75 / ×1.3, выучено при ≤0.1), активные порции с автодобором, статистика и streak.
- **25-й кадр (RSVP)**: настраиваемая скорость (100–3000 мс), длительность (1–10 мин),
  режимы прямой/обратный/смешанный, настройки сохраняются.
- **Разбор текстов**: библиотека статей с фильтрами (язык/уровень/тег), вставка своего текста
  (до 10 000 символов), клик по слову → перевод (кэш 30 дней) → добавление в словарь.
- **Монетизация**: Premium-подписка 1/6/12 мес (мок-шлюз для локальной разработки,
  цены в админке), Paywall при превышении лимита и доступе к C1/C2,
  реклама для Free-пользователей (вкл/выкл и ID блоков в админке).
- **Админка**: пользователи (поиск, Premium, блокировка), системные настройки,
  импорт словарей из JSON с валидацией, CRUD статей с проверкой на нецензурную лексику.
- **Безопасность**: BCrypt, JWT (jjwt 0.12), rate limiting (bucket4j), Actuator health,
  CORS, валидация входных данных.

## API

Swagger UI: http://localhost:8087/swagger-ui.html

## Тесты

```bash
mvn test
```

## Миграция данных

Liquibase changelog `db.changelog-6.0.yaml` аддитивно мигрирует существующую базу:
переносит `word_progress` в `user_dictionary`, теги из JSON-строк в `word_tags`,
тексты из `texts` в `articles`, добавляет premium-поля и `level_order`.
Старые таблицы не удаляются.
