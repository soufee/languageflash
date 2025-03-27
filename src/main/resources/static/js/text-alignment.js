/**
 * text-alignment.js
 * Синхронизация положения абзацев в viewTextEn и viewTextRu по верхней границе
 * с разделением абзацев в обеих колонках
 */

function syncTextAlignment() {
    console.log('[text-alignment.js] Синхронизация положения абзацев с разделением...');

    const enContainer = document.getElementById('viewTextEn');
    const ruContainer = document.getElementById('viewTextRu');

    if (!enContainer || !ruContainer) {
        console.error('[text-alignment.js] Один из контейнеров не найден');
        return;
    }

    // Собираем все абзацы с data-match-id
    const enParagraphs = Array.from(enContainer.querySelectorAll('p[data-match-id]'));
    const ruParagraphs = Array.from(ruContainer.querySelectorAll('p[data-match-id]'));

    if (enParagraphs.length !== ruParagraphs.length) {
        console.warn('[text-alignment.js] Количество абзацев в колонках не совпадает');
        return;
    }

    // Сбрасываем все дополнительные отступы перед пересчетом
    enParagraphs.forEach(p => p.style.marginBottom = '0');
    ruParagraphs.forEach(p => p.style.marginBottom = '0');

    // Базовый отступ между абзацами (можно настроить)
    const baseGap = 10; // px

    // Проходим по всем парам абзацев
    for (let i = 0; i < enParagraphs.length; i++) {
        const enP = enParagraphs[i];
        const ruP = ruParagraphs[i];
        const matchId = enP.getAttribute('data-match-id');

        if (matchId !== ruP.getAttribute('data-match-id')) {
            console.warn(`[text-alignment.js] Несоответствие data-match-id на позиции ${i}: ${matchId}`);
            continue;
        }

        // Получаем высоту каждого абзаца
        const enHeight = enP.offsetHeight;
        const ruHeight = ruP.offsetHeight;
        const maxHeight = Math.max(enHeight, ruHeight);

        // Устанавливаем высоту и добавляем базовый отступ
        if (enHeight < maxHeight) {
            const diff = maxHeight - enHeight;
            enP.style.marginBottom = `${diff + baseGap}px`;
        } else {
            enP.style.marginBottom = `${baseGap}px`; // Базовый отступ для равных или более высоких
        }

        if (ruHeight < maxHeight) {
            const diff = maxHeight - ruHeight;
            ruP.style.marginBottom = `${diff + baseGap}px`;
        } else {
            ruP.style.marginBottom = `${baseGap}px`; // Базовый отступ для равных или более высоких
        }

        console.log(`[text-alignment.js] Абзац ${matchId}: enHeight=${enHeight}, ruHeight=${ruHeight}, maxHeight=${maxHeight}`);
    }

    console.log('[text-alignment.js] Синхронизация завершена');
}

// Выполняем синхронизацию после загрузки модального окна
function initTextAlignment() {
    const modalElement = document.getElementById('viewTextModal');
    if (!modalElement) {
        console.error('[text-alignment.js] Модальное окно не найдено');
        return;
    }

    // Запускаем синхронизацию при открытии модального окна
    modalElement.addEventListener('shown.bs.modal', () => {
        syncTextAlignment();
        // Повторяем синхронизацию через небольшую задержку для учета динамической загрузки шрифтов или контента
        setTimeout(syncTextAlignment, 100);
    });

    // Пересчитываем при изменении размера окна
    window.addEventListener('resize', syncTextAlignment);
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    console.log('[text-alignment.js] Инициализация скрипта...');
    initTextAlignment();
});