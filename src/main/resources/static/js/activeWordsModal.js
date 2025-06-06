function loadActiveWords() {
    console.log('=== loadActiveWords START ===');
    
    // Check if currentUserId is defined
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        document.getElementById('activeWordsBody').innerHTML = '<tr><td colspan="3">Ошибка: ID пользователя не найден</td></tr>';
        return;
    }
    
    const url = `/api/dashboard/words/active?userId=${currentUserId}`;
    console.log('Fetching active words from:', url);
    
    fetch(url, {
        method: 'GET',
        credentials: 'include'
    })
        .then(response => {
            console.log('Response status:', response.status);
            if (!response.ok) {
                throw new Error('Ошибка загрузки активных слов: ' + response.status);
            }
            return response.json(); // Используем response.json() вместо response.text()
        })
        .then(words => {
            console.log('Active words received:', words);
            const tbody = document.getElementById('activeWordsBody');
            const noWordsMessage = document.getElementById('noActiveWords');
            const refillButton = document.getElementById('refillButton');

            tbody.innerHTML = '';

            // Получаем лимит активных слов из настроек
            getActiveWordsCount()
                .then(activeWordsCount => {
                    console.log('Active words count limit:', activeWordsCount);
                    if (words.length === 0) {
                        noWordsMessage.style.display = 'block';
                        if (refillButton) refillButton.disabled = false;
                    } else {
                        noWordsMessage.style.display = 'none';
                        // Кнопка активна, если слов меньше лимита
                        if (refillButton) refillButton.disabled = words.length >= activeWordsCount;
                    }

                    words.forEach(wordProgress => {
                        const word = wordProgress.word; // WordProgress содержит объект word
                        const row = document.createElement('tr');
                        row.innerHTML = `
                            <td>${word.word}</td>
                            <td>${word.translation}</td>
                            <td><button class="btn btn-sm btn-danger" onclick="removeWord(${word.id})">Удалить</button></td>
                        `;
                        tbody.appendChild(row);
                    });
                })
                .catch(error => {
                    console.error('Ошибка получения лимита активных слов:', error);
                    // В случае ошибки оставляем старую логику как запасной вариант
                    if (words.length === 0) {
                        noWordsMessage.style.display = 'block';
                        if (refillButton) refillButton.disabled = false;
                    } else {
                        noWordsMessage.style.display = 'none';
                        if (refillButton) refillButton.disabled = true;
                    }
                });
        })
        .catch(error => {
            console.error('Ошибка:', error);
            document.getElementById('activeWordsBody').innerHTML = '<tr><td colspan="3">Ошибка загрузки слов</td></tr>';
        });
}

function removeWord(wordId) {
    console.log('=== removeWord called for wordId:', wordId, '===');
    
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        alert('Ошибка: ID пользователя не найден');
        return;
    }
    
    const url = `/api/dashboard/words/remove?userId=${currentUserId}&wordId=${wordId}`;
    console.log('Removing word via:', url);
    
    fetch(url, {
        method: 'POST',
        credentials: 'include'
    })
        .then(response => {
            console.log('Remove word response status:', response.status);
            if (!response.ok) {
                throw new Error('Ошибка удаления слова: ' + response.status);
            }
            console.log('Word removed successfully');
            loadActiveWords(); // Обновляем список активных слов
            if (typeof updateDashboardCounts === 'function') {
                updateDashboardCounts(); // Обновляем счетчики на дашборде
            }
        })
        .catch(error => {
            console.error('Ошибка удаления слова:', error);
            alert('Ошибка при удалении слова: ' + error.message);
        });
}

function refillActiveWords() {
    console.log('=== refillActiveWords called ===');
    
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        alert('Ошибка: ID пользователя не найден');
        return;
    }
    
    // Получаем настройки пользователя для подгрузки слов
    fetch(`/api/dashboard/settings?userId=${currentUserId}`, {
        credentials: 'include'
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Ошибка получения настроек: ' + response.status);
            }
            return response.json();
        })
        .then(settings => {
            console.log('User settings:', settings);
            
            const language = settings.language;
            const minLevel = settings.minLevel;
            const tags = settings.tags || [];
            
            if (!language || !minLevel) {
                alert('Для пополнения слов необходимо настроить программу обучения');
                return;
            }
            
            const url = `/api/dashboard/words/refill?userId=${currentUserId}&language=${encodeURIComponent(language)}&minLevel=${encodeURIComponent(minLevel)}${tags.length > 0 ? '&tags=' + tags.map(encodeURIComponent).join('&tags=') : ''}`;
            console.log('Refilling words via:', url);
            
            return fetch(url, {
                method: 'POST',
                credentials: 'include'
            });
        })
        .then(response => {
            console.log('Refill response status:', response.status);
            if (!response.ok) {
                throw new Error('Ошибка подгрузки слов: ' + response.status);
            }
            console.log('Words refilled successfully');
            loadActiveWords(); // Обновляем список после подгрузки
            if (typeof updateDashboardCounts === 'function') {
                updateDashboardCounts(); // Обновляем счетчики
            }
        })
        .catch(error => {
            console.error('Ошибка подгрузки слов:', error);
            alert('Ошибка при подгрузке слов: ' + error.message);
        });
}

document.addEventListener('DOMContentLoaded', function () {
    try {
        const modalElement = document.getElementById('activeWordsModal');
        if (modalElement) {
            console.log('ActiveWordsModal: инициализация модального окна');
            modalElement.addEventListener('shown.bs.modal', function () {
                loadActiveWords();
            });
        } else {
            console.log("ActiveWordsModal: элемент #activeWordsModal не найден в DOM");
        }
    } catch (error) {
        console.error('Ошибка при инициализации activeWordsModal:', error);
    }
});