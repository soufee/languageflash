/**
 * Пользовательские функции для страницы текстов
 */

document.addEventListener('DOMContentLoaded', function() {

    // Инициализация всплывающих подсказок Bootstrap, если они есть на странице
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    if (tooltipTriggerList.length > 0) {
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }
    
    // Проверяем модальные окна только на странице dashboard
    if (window.location.pathname === '/dashboard') {
        // Проверяем наличие модальных окон
        const modalIds = [
            'learnModal',
            'flashLearnModal',
            'activeWordsModal',
            'learnedWordsModal',
            'textWordsModal'
        ];
        
        let missingModals = [];
        modalIds.forEach(id => {
            if (!document.getElementById(id)) {
                missingModals.push(id);
            }
        });
        
        if (missingModals.length > 0) {
            console.log('Отсутствующие модальные окна на странице dashboard:', missingModals);
            
            // Создаем обертку для кнопок
            document.addEventListener('click', function(e) {
                const target = e.target;
                if (target.hasAttribute('data-bs-target')) {
                    const targetId = target.getAttribute('data-bs-target').replace('#', '');
                    if (missingModals.includes(targetId)) {
                        e.preventDefault();
                        e.stopPropagation();
                        showNotification(`Это действие недоступно. Пожалуйста, обновите страницу.`, 'warning');
                        return false;
                    }
                }
            }, true);
        }
    }
});

// Вспомогательная функция для показа уведомлений
function showNotification(message, type) {
    const alertContainer = document.getElementById('alertContainer');
    if (!alertContainer) {
        console.warn('Контейнер для уведомлений не найден');
        return;
    }
    
    const alert = document.createElement('div');
    alert.className = `alert alert-${type || 'info'} alert-dismissible fade show`;
    alert.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Закрыть"></button>
    `;
    
    alertContainer.appendChild(alert);
    
    // Автоматически скрываем уведомление через 5 секунд
    setTimeout(() => {
        alert.classList.remove('show');
        setTimeout(() => {
            alertContainer.removeChild(alert);
        }, 300);
    }, 5000);
} 