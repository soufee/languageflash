/**
 * Глобальные функции для работы со словами из текстов
 */

// Хранилище для данных
window.textWordsData = {
    allTextWords: [],
    textsList: []
};

/**
 * Загружает слова из текстов
 */
window.loadTextWords = function() {
    console.log("[text-words.js] Загрузка слов из текстов...");
    
    // Проверка наличия всех необходимых элементов
    const loadingIndicator = document.getElementById('loadingIndicator');
    const textWordsContent = document.getElementById('textWordsContent');
    const noTextWordsMessage = document.getElementById('noTextWordsMessage');
    const errorMessage = document.getElementById('errorMessage');
    const textWordsLoadStatus = document.getElementById('textWordsLoadStatus');
    
    // Отображаем индикатор загрузки
    if (loadingIndicator) loadingIndicator.style.display = 'block';
    if (textWordsContent) textWordsContent.style.display = 'none';
    if (noTextWordsMessage) noTextWordsMessage.style.display = 'none';
    if (errorMessage) errorMessage.style.display = 'none';
    
    // Прямая отладка - проверяем состояние DOM-элементов
    console.log("DOM-элементы:");
    console.log("- loadingIndicator:", loadingIndicator);
    console.log("- textWordsContent:", textWordsContent);
    console.log("- noTextWordsMessage:", noTextWordsMessage);
    console.log("- errorMessage:", errorMessage);
    console.log("- textWordsLoadStatus:", textWordsLoadStatus);
    
    // Проверяем URL и добавляем метку времени для предотвращения кэширования
    const url = '/dashboard/text-words?_=' + new Date().getTime();
    console.log("Запрос к URL:", url);
    
    try {
        if (textWordsLoadStatus) {
            textWordsLoadStatus.textContent = 'Отправка запроса к ' + url;
        }
        
        fetch(url)
            .then(response => {
                console.log("Получен ответ:", response.status, response.statusText);
                if (textWordsLoadStatus) {
                    textWordsLoadStatus.textContent = 'Статус ответа: ' + response.status;
                }
                
                if (!response.ok) {
                    if (response.status === 401) {
                        console.log("Пользователь не авторизован");
                        return { words: [], texts: [] };
                    }
                    throw new Error("Ошибка при загрузке данных: " + response.status);
                }
                return response.json();
            })
            .then(data => {
                // Скрываем индикатор загрузки
                if (loadingIndicator) loadingIndicator.style.display = 'none';
                
                console.log("Получены данные:", JSON.stringify(data).substring(0, 100) + "...");
                if (textWordsLoadStatus) {
                    textWordsLoadStatus.textContent = 'Данные получены';
                }
                
                // Отдельно проверяем структуру ответа
                console.log("Тип данных:", typeof data);
                console.log("Свойства ответа:", Object.keys(data));
                
                // Если данные пришли в разных форматах, обрабатываем все варианты
                let words = [];
                let texts = [];
                
                if (Array.isArray(data)) {
                    console.log("Данные пришли в виде массива");
                    words = data;
                } else if (typeof data === 'object') {
                    console.log("Данные пришли в виде объекта");
                    words = data.words || [];
                    texts = data.texts || [];
                    
                    // Если в данных есть другие свойства, пробуем их использовать
                    if (words.length === 0 && Array.isArray(data.textWords)) {
                        console.log("Используем поле textWords");
                        words = data.textWords;
                    }
                }
                
                // Сохраняем данные в глобальное хранилище
                window.textWordsData.allTextWords = words;
                window.textWordsData.textsList = texts;
                
                console.log("Количество слов:", words.length);
                console.log("Количество текстов:", texts.length);
                
                // Проверяем, есть ли ошибка в ответе
                if (data.error) {
                    console.error("Ошибка от сервера:", data.error);
                    if (errorMessage) {
                        errorMessage.textContent = data.error;
                        errorMessage.style.display = 'block';
                    }
                    return;
                }
                
                if (!textWordsContent || !noTextWordsMessage) {
                    console.error("Не найдены ключевые элементы DOM для отображения данных");
                    return;
                }
                
                if (!words || words.length === 0) {
                    console.log("Нет слов для отображения");
                    textWordsContent.style.display = 'none';
                    noTextWordsMessage.style.display = 'block';
                    
                    // Добавляем дополнительное сообщение в статус
                    if (textWordsLoadStatus) {
                        textWordsLoadStatus.textContent = 'Нет слов для отображения';
                    }
                } else {
                    console.log("Найдено слов:", words.length);
                    textWordsContent.style.display = 'block';
                    noTextWordsMessage.style.display = 'none';
                    
                    // Заполняем селект с текстами
                    const textFilter = document.getElementById('textFilter');
                    if (textFilter) {
                        textFilter.innerHTML = '<option value="">Все тексты</option>';
                        
                        texts.forEach(text => {
                            console.log("Добавляем текст в фильтр:", text.id, text.title);
                            const option = document.createElement('option');
                            option.value = text.id;
                            option.textContent = text.title;
                            textFilter.appendChild(option);
                        });
                    }
                    
                    // Отображаем все слова
                    window.displayTextWords(words);
                    
                    // Обновляем статус
                    if (textWordsLoadStatus) {
                        textWordsLoadStatus.textContent = 'Отображено слов: ' + words.length;
                    }
                }
            })
            .catch(error => {
                // Скрываем индикатор загрузки
                if (loadingIndicator) loadingIndicator.style.display = 'none';
                
                console.error('Ошибка при загрузке слов из текстов:', error);
                if (errorMessage) {
                    errorMessage.textContent = 'Произошла ошибка при загрузке слов: ' + error.message;
                    errorMessage.style.display = 'block';
                }
                
                // Обновляем статус
                if (textWordsLoadStatus) {
                    textWordsLoadStatus.textContent = 'Ошибка: ' + error.message;
                }
            });
    } catch (error) {
        // Скрываем индикатор загрузки
        if (loadingIndicator) loadingIndicator.style.display = 'none';
        
        console.error('Критическая ошибка при загрузке слов из текстов:', error);
        if (errorMessage) {
            errorMessage.textContent = 'Критическая ошибка: ' + error.message;
            errorMessage.style.display = 'block';
        }
        
        // Обновляем статус
        if (textWordsLoadStatus) {
            textWordsLoadStatus.textContent = 'Критическая ошибка: ' + error.message;
        }
    }
};

/**
 * Отображает слова из текстов в таблице
 * @param {Array} words - Массив слов для отображения
 */
window.displayTextWords = function(words) {
    console.log("[text-words.js] Отображение слов:", words.length);
    
    const tbody = document.getElementById('textWordsList');
    if (!tbody) {
        console.error("Элемент textWordsList не найден!");
        return;
    }
    
    tbody.innerHTML = '';
    
    words.forEach(word => {
        // Пропускаем уже изученные слова
        if (word.learned) return;
        
        const row = document.createElement('tr');
        row.dataset.wordId = word.id;
        row.innerHTML = `
            <td>${word.word}</td>
            <td>${word.translation}</td>
            <td>${word.textTitle || 'Неизвестно'}</td>
            <td>
                <button class="btn btn-sm btn-danger mark-as-learned" 
                        onclick="window.markAsLearned(${word.id})">
                    Удалить
                </button>
            </td>
        `;
        tbody.appendChild(row);
    });
    
    console.log("Отображение завершено, добавлено строк:", tbody.children.length);
};

/**
 * Отмечает слово как изученное и удаляет его из списка активных слов
 * @param {Number} wordId - ID слова, которое нужно отметить как изученное
 */
window.markAsLearned = function(wordId) {
    console.log("[text-words.js] Отметка слова как изученного:", wordId);
    
    const loadingIndicator = document.getElementById('loadingIndicator');
    const textWordsLoadStatus = document.getElementById('textWordsLoadStatus');
    
    // Показываем индикатор загрузки
    if (loadingIndicator) loadingIndicator.style.display = 'block';
    if (textWordsLoadStatus) textWordsLoadStatus.textContent = 'Обновление статуса слова...';
    
    // Отправляем запрос на обновление статуса слова
    fetch('/api/dashboard/update-progress', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            wordId: wordId,
            markLearned: true
        })
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Ошибка при обновлении статуса слова: ' + response.status);
        }
        return response.json();
    })
    .then(data => {
        // Скрываем индикатор загрузки
        if (loadingIndicator) loadingIndicator.style.display = 'none';
        
        console.log("Ответ сервера:", data);
        
        if (data.status === 'success') {
            // Обновляем отображаемый список слов
            const row = document.querySelector(`tr[data-word-id="${wordId}"]`);
            if (row) {
                row.classList.add('fade-out');
                setTimeout(() => {
                    row.remove();
                    
                    // Обновляем счетчики
                    updateWordCounters();
                    
                    // Проверяем, остались ли слова
                    const tbody = document.getElementById('textWordsList');
                    if (tbody && tbody.children.length === 0) {
                        const textWordsContent = document.getElementById('textWordsContent');
                        const noTextWordsMessage = document.getElementById('noTextWordsMessage');
                        
                        if (textWordsContent) textWordsContent.style.display = 'none';
                        if (noTextWordsMessage) noTextWordsMessage.style.display = 'block';
                        if (textWordsLoadStatus) textWordsLoadStatus.textContent = 'Все слова отмечены как изученные';
                    } else {
                        if (textWordsLoadStatus) textWordsLoadStatus.textContent = 'Слово успешно отмечено как изученное';
                    }
                }, 300); // Анимация удаления
            }
            
            // Обновляем данные в хранилище
            const words = window.textWordsData.allTextWords;
            const wordIndex = words.findIndex(w => w.id === wordId);
            if (wordIndex !== -1) {
                words[wordIndex].learned = true;
                words[wordIndex].knowledgeFactor = 0;
            }
        } else {
            if (textWordsLoadStatus) textWordsLoadStatus.textContent = 'Ошибка: ' + (data.message || 'Неизвестная ошибка');
        }
    })
    .catch(error => {
        // Скрываем индикатор загрузки
        if (loadingIndicator) loadingIndicator.style.display = 'none';
        
        console.error('Ошибка при отметке слова как изученного:', error);
        if (textWordsLoadStatus) textWordsLoadStatus.textContent = 'Ошибка: ' + error.message;
    });
};

/**
 * Обновляет счетчики слов на странице дашборда
 */
function updateWordCounters() {
    // Обновляем счетчик активных слов из текстов
    fetchTextWordsCount();
    
    // Обновляем счетчик изученных слов
    fetch('/api/dashboard/learned-words-count')
        .then(response => response.json())
        .then(data => {
            const learnedWordsCount = document.getElementById('learnedWordsCount');
            if (learnedWordsCount) {
                learnedWordsCount.textContent = data.count;
            }
        })
        .catch(error => {
            console.error('Ошибка при обновлении счетчика изученных слов:', error);
        });
}

/**
 * Фильтрует слова по выбранному тексту
 */
window.filterTextWords = function() {
    const textId = document.getElementById('textFilter')?.value;
    const words = window.textWordsData.allTextWords;
    
    if (!textId) {
        // Показываем все слова
        window.displayTextWords(words);
    } else {
        // Фильтруем слова по выбранному тексту
        const filteredWords = words.filter(word => word.textId == textId);
        window.displayTextWords(filteredWords);
    }
}; 