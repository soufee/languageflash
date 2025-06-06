/**
 * JavaScript для админки - управление пользователями
 */

document.addEventListener('DOMContentLoaded', function() {
    initUserManagement();
    initUserSearch();
});

function initUserManagement() {
    // Обработка блокировки/разблокировки пользователей
    document.querySelectorAll('.block-user-btn').forEach(button => {
        button.addEventListener('click', handleBlockUser);
    });
    
    // Обработка назначения/снятия роли администратора
    document.querySelectorAll('.toggle-admin-btn').forEach(button => {
        button.addEventListener('click', handleToggleAdmin);
    });
}

function initUserSearch() {
    const searchForm = document.querySelector('form[action*="/api/admin/users/search"]');
    if (searchForm) {
        searchForm.addEventListener('submit', handleUserSearch);
    }
    
    const showAllButton = document.getElementById('show-all-users');
    if (showAllButton) {
        showAllButton.addEventListener('click', handleShowAllUsers);
    }
}

function handleBlockUser() {
    const userId = this.dataset.userId;
    const isBlocked = this.dataset.blocked === 'true';
    const newBlockedState = !isBlocked;
    
    // Отключаем кнопку во время запроса
    this.disabled = true;
    const originalText = this.textContent;
    this.textContent = 'Обработка...';
    
    fetch('/api/admin/users/block', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `userId=${userId}&blocked=${newBlockedState}`
    })
    .then(response => {
        if (response.ok) {
            // Обновляем состояние кнопки
            this.dataset.blocked = newBlockedState.toString();
            this.textContent = newBlockedState ? 'Разблокировать' : 'Заблокировать';
            
            // Обновляем статус в таблице
            const row = this.closest('tr');
            const statusCell = row.querySelector('td:nth-child(4)');
            statusCell.textContent = newBlockedState ? 'Заблокирован' : 'Активен';
            
            // Показываем уведомление об успехе
            showNotification('Статус пользователя успешно изменен', 'success');
        } else {
            throw new Error('Ошибка сервера');
        }
    })
    .catch(error => {
        console.error('Ошибка:', error);
        this.textContent = originalText;
        showNotification('Ошибка при изменении статуса пользователя', 'error');
    })
    .finally(() => {
        this.disabled = false;
    });
}

function handleToggleAdmin() {
    const userId = this.dataset.userId;
    const isAdmin = this.dataset.isAdmin === 'true';
    const newAdminState = !isAdmin;
    
    // Отключаем кнопку во время запроса
    this.disabled = true;
    const originalText = this.textContent;
    this.textContent = 'Обработка...';
    
    fetch('/api/admin/users/toggle-admin', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `userId=${userId}&isAdmin=${newAdminState}`
    })
    .then(response => {
        if (response.ok) {
            // Обновляем состояние кнопки
            this.dataset.isAdmin = newAdminState.toString();
            this.textContent = newAdminState ? 'Убрать ADMIN' : 'Дать ADMIN';
            
            // Обновляем роли в таблице
            const row = this.closest('tr');
            const rolesCell = row.querySelector('td:nth-child(3)');
            const currentRoles = rolesCell.textContent;
            
            if (newAdminState) {
                // Добавляем роль ADMIN
                if (!currentRoles.includes('ADMIN')) {
                    rolesCell.textContent = currentRoles.includes('USER') ? 
                        currentRoles.replace('USER', 'USER, ADMIN') : 'ADMIN';
                }
            } else {
                // Убираем роль ADMIN
                rolesCell.textContent = currentRoles.replace(/,?\s*ADMIN/g, '').replace(/ADMIN,?\s*/g, '').trim();
                if (rolesCell.textContent === '') {
                    rolesCell.textContent = 'USER';
                }
            }
            
            // Показываем уведомление об успехе
            showNotification('Роль администратора успешно изменена', 'success');
        } else {
            throw new Error('Ошибка сервера');
        }
    })
    .catch(error => {
        console.error('Ошибка:', error);
        this.textContent = originalText;
        showNotification('Ошибка при изменении роли администратора', 'error');
    })
    .finally(() => {
        this.disabled = false;
    });
}

function handleUserSearch(event) {
    event.preventDefault();
    
    const form = event.target;
    const formData = new FormData(form);
    const email = formData.get('email');
    
    if (!email || email.trim() === '') {
        showNotification('Введите email для поиска', 'error');
        return;
    }
    
    // Отключаем кнопку поиска
    const submitButton = form.querySelector('button[type="submit"]');
    submitButton.disabled = true;
    const originalText = submitButton.textContent;
    submitButton.textContent = 'Поиск...';
    
    fetch(`/api/admin/users/search?email=${encodeURIComponent(email)}`)
    .then(response => {
        if (!response.ok) {
            throw new Error('Ошибка поиска');
        }
        return response.json();
    })
    .then(data => {
        renderSearchResults(data);
        showNotification(`Найдено пользователей: ${data.totalElements}`, 'success');
        
        // Очищаем поле поиска
        form.querySelector('input[name="email"]').value = '';
    })
    .catch(error => {
        console.error('Ошибка поиска:', error);
        showNotification('Ошибка при поиске пользователей', 'error');
    })
    .finally(() => {
        submitButton.disabled = false;
        submitButton.textContent = originalText;
    });
}

function handleShowAllUsers() {
    const button = document.getElementById('show-all-users');
    button.disabled = true;
    const originalText = button.textContent;
    button.textContent = 'Загрузка...';
    
    fetch('/api/admin/users')
    .then(response => {
        if (!response.ok) {
            throw new Error('Ошибка загрузки');
        }
        return response.json();
    })
    .then(data => {
        renderSearchResults(data);
        showNotification(`Показано пользователей: ${data.totalElements}`, 'success');
    })
    .catch(error => {
        console.error('Ошибка загрузки пользователей:', error);
        showNotification('Ошибка при загрузке списка пользователей', 'error');
    })
    .finally(() => {
        button.disabled = false;
        button.textContent = originalText;
    });
}

function renderSearchResults(pageData) {
    const tbody = document.querySelector('table tbody');
    const currentUserId = getCurrentUserId();
    
    // Очищаем текущие результаты
    tbody.innerHTML = '';
    
    if (pageData.content.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">Пользователи не найдены</td></tr>';
        return;
    }
    
    // Отрисовываем найденных пользователей
    pageData.content.forEach(user => {
        const row = createUserRow(user, currentUserId);
        tbody.appendChild(row);
    });
    
    // Переинициализируем обработчики событий для новых кнопок
    initUserManagement();
}

function createUserRow(user, currentUserId) {
    const row = document.createElement('tr');
    if (user.id === currentUserId) {
        row.classList.add('current-admin');
    }
    
    const isCurrentUser = user.id === currentUserId;
    const isBlocked = user.blocked;
    const isAdmin = user.roles && user.roles.includes('ADMIN');
    
    row.innerHTML = `
        <td>${user.email}</td>
        <td>${user.firstName || ''}</td>
        <td>${user.roles ? user.roles.join(', ') : 'USER'}</td>
        <td>${isBlocked ? 'Заблокирован' : 'Активен'}</td>
        <td>
            ${!isCurrentUser ? `
                <button type="button" 
                        class="btn btn-danger btn-sm block-user-btn" 
                        data-user-id="${user.id}"
                        data-blocked="${isBlocked}">
                    ${isBlocked ? 'Разблокировать' : 'Заблокировать'}
                </button>
                <button type="button" 
                        class="btn btn-warning btn-sm toggle-admin-btn" 
                        data-user-id="${user.id}"
                        data-is-admin="${isAdmin}">
                    ${isAdmin ? 'Убрать ADMIN' : 'Дать ADMIN'}
                </button>
            ` : '-'}
        </td>
    `;
    
    return row;
}

function getCurrentUserId() {
    // Получаем ID текущего пользователя из строки с классом current-admin
    const currentAdminRow = document.querySelector('tr.current-admin');
    if (currentAdminRow) {
        const blockBtn = currentAdminRow.querySelector('.block-user-btn');
        const toggleBtn = currentAdminRow.querySelector('.toggle-admin-btn');
        if (blockBtn) {
            return parseInt(blockBtn.dataset.userId);
        }
        if (toggleBtn) {
            return parseInt(toggleBtn.dataset.userId);
        }
    }
    return null;
}

function showNotification(message, type) {
    // Создаем элемент уведомления
    const notification = document.createElement('div');
    notification.className = `alert alert-${type === 'success' ? 'success' : 'danger'} alert-dismissible fade show position-fixed`;
    notification.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
    notification.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    
    // Добавляем в body
    document.body.appendChild(notification);
    
    // Автоматически убираем через 5 секунд
    setTimeout(() => {
        if (notification.parentNode) {
            notification.remove();
        }
    }, 5000);
} 