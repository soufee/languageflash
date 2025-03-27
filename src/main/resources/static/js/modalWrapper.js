/**
 * Wrapper для инициализации модальных окон
 * Предотвращает ошибки, если модальное окно не найдено на странице
 */

// Оборачиваем инициализацию для предотвращения ошибок
document.addEventListener('DOMContentLoaded', function() {
    console.log('modalWrapper.js: Инициализация модальных окон');
    
    // Модальные окна, которые могут отсутствовать на некоторых страницах
    const modalIds = [
        'learnModal',
        'flashLearnModal',
        'activeWordsModal',
        'learnedWordsModal',
        'textWordsModal',
        'customWordsModal',
        'programModal',
        'addTagsModal'
    ];
    
    // Проверяем наличие каждого модального окна
    modalIds.forEach(modalId => {
        const modalElement = document.getElementById(modalId);
        if (!modalElement) {
            console.log(`modalWrapper.js: Модальное окно #${modalId} не найдено на странице`);
            // Создаем заглушку для предотвращения ошибок
            window[`init${modalId.charAt(0).toUpperCase() + modalId.slice(1)}`] = function() {
                console.log(`modalWrapper.js: Вызвана заглушка для #${modalId}`);
                return false;
            };
        } else {
            console.log(`modalWrapper.js: Найдено модальное окно #${modalId}`);
        }
    });
});

// Функция-заглушка для addEventListener
function safeAddEventListener(elementId, event, handler) {
    const element = document.getElementById(elementId);
    if (element) {
        element.addEventListener(event, handler);
        return true;
    } else {
        console.log(`modalWrapper.js: Элемент #${elementId} не найден, addEventListener пропущен`);
        return false;
    }
} 