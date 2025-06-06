// Функция для получения ID текущего пользователя
function getUserId() {
    return window.currentUserId || currentUserId;
}

// Функция для получения количества слов из текстов
function fetchTextWordsCount() {
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        const countElement = document.getElementById('textWordsCount');
        if (countElement) {
            countElement.textContent = '0';
        }
        return;
    }

    fetch(`/api/dashboard/texts/words/count/all?userId=${currentUserId}`)
        .then(response => {
            if (!response.ok) {
                if (response.status === 401) {
                    return 0;
                }
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(count => {
            const countElement = document.getElementById('textWordsCount');
            if (countElement) {
                countElement.textContent = count;
            }
        })
        .catch(error => {
            console.error('Ошибка при загрузке количества слов из текстов:', error);
            const countElement = document.getElementById('textWordsCount');
            if (countElement) {
                countElement.textContent = '0';
            }
        });
}

function updateDashboardCounts() {
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        return;
    }

    fetch(`/api/dashboard/words/active?userId=${currentUserId}`)
        .then(response => response.text())
        .then(data => {
            const activeWords = JSON.parse(data);
            const activeCount = activeWords.length;
            document.getElementById('activeWordsCount').textContent = activeCount;

            fetch(`/api/dashboard/words/learned?userId=${currentUserId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            })
                .then(response => response.text())
                .then(data => {
                    const learnedWords = JSON.parse(data);
                    const learnedCount = learnedWords.length;
                    document.getElementById('learnedWordsCount').textContent = learnedCount;

                    fetch(`/api/dashboard/words/custom?userId=${currentUserId}`)
                        .then(response => response.json())
                        .then(customWords => {
                            document.getElementById('customWordsCount').textContent = customWords.length;
                        });
                })
                .catch(error => {});
        })
        .catch(error => {});
}

function loadCustomWords() {
    console.log('=== loadCustomWords START ===');
    
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        return;
    }

    const loadingIndicator = document.getElementById('loadingIndicator');
    const tbody = document.getElementById('customWordsBody');
    const noWordsMessage = document.getElementById('noCustomWords');
    
    // Показываем индикатор загрузки
    if (loadingIndicator) {
        loadingIndicator.classList.remove('d-none');
    }
    
    // Очищаем предыдущие данные
    tbody.innerHTML = '';
    noWordsMessage.style.display = 'none';
    
    const url = `/api/dashboard/words/custom?userId=${currentUserId}`;
    console.log('Fetching custom words from:', url);
    
    fetch(url, {
        method: 'GET',
        credentials: 'include'
    })
        .then(response => {
            console.log('Custom words response status:', response.status);
            if (!response.ok) {
                throw new Error('Ошибка загрузки кастомных слов: ' + response.status);
            }
            return response.json();
        })
        .then(words => {
            console.log('Custom words received:', words);
            if (words.length === 0) {
                console.log('No custom words found');
                noWordsMessage.style.display = 'block';
            } else {
                console.log('Displaying', words.length, 'custom words');
                noWordsMessage.style.display = 'none';
                words.forEach(wordProgress => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${wordProgress.word.word}</td>
                        <td>${wordProgress.word.translation}</td>
                    `;
                    tbody.appendChild(row);
                });
            }
        })
        .catch(error => {
            console.error('Ошибка загрузки слов:', error);
            tbody.innerHTML = '<tr><td colspan="2" class="text-center text-danger">Ошибка загрузки слов</td></tr>';
        })
        .finally(() => {
            // Скрываем индикатор загрузки
            if (loadingIndicator) {
                loadingIndicator.classList.add('d-none');
            }
            console.log('=== loadCustomWords END ===');
        });
}

function showAddCustomWordForm() {
    document.getElementById('addCustomWordForm').style.display = 'block';
    document.getElementById('customWord').value = '';
    document.getElementById('customTranslation').value = '';
    document.getElementById('customExample').value = '';
    document.getElementById('customExampleTranslation').value = '';
    document.getElementById('customWordWarning').style.display = 'none';
    document.getElementById('saveCustomWordButton').disabled = true;
    
    setTimeout(() => {
        document.getElementById('customWord').focus();
    }, 100);
}

function hideAddCustomWordForm() {
    document.getElementById('addCustomWordForm').style.display = 'none';
}

function checkSaveButtonState() {
    const word = document.getElementById('customWord').value.trim();
    const translation = document.getElementById('customTranslation').value.trim();
    const saveButton = document.getElementById('saveCustomWordButton');
    saveButton.disabled = !(word && translation);
}

function checkAutocomplete() {
    const word = document.getElementById('customWord').value.trim();
    const saveButton = document.getElementById('saveCustomWordButton');
    const warningDiv = document.getElementById('customWordWarning');

    warningDiv.style.display = 'none';
    saveButton.disabled = true;

    if (!word) {
        return;
    }

    if (checkAutocomplete.timer) {
        clearTimeout(checkAutocomplete.timer);
    }

    checkSaveButtonState();

    checkAutocomplete.timer = setTimeout(() => {
        fetch(`/api/dashboard/words/autocomplete?word=${encodeURIComponent(word)}`, {
            credentials: 'include'
        })
            .then(response => response.json())
            .then(suggestions => {
                // Автокомплит возвращает массив предложений, но здесь нужна другая логика
                // Пока просто проверяем состояние кнопки
                checkSaveButtonState();
            })
            .catch(error => {
                checkSaveButtonState();
            });
    }, 300);
}

function checkDuplicates() {
    const word = document.getElementById('customWord').value.trim();
    const translation = document.getElementById('customTranslation').value.trim();
    const saveButton = document.getElementById('saveCustomWordButton');
    const warningDiv = document.getElementById('customWordWarning');

    warningDiv.style.display = 'none';
    saveButton.disabled = true;

    if (!word || !translation) {
        checkSaveButtonState();
        return;
    }

    if (checkDuplicates.timer) {
        clearTimeout(checkDuplicates.timer);
    }

    checkSaveButtonState();

    checkDuplicates.timer = setTimeout(() => {
        fetch(`/api/dashboard/words/duplicate?word=${encodeURIComponent(word)}&translation=${encodeURIComponent(translation)}`, {
            credentials: 'include'
        })
            .then(response => response.json())
            .then(isDuplicate => {
                if (isDuplicate) {
                    warningDiv.className = 'alert alert-warning';
                    warningDiv.textContent = 'Такое слово уже существует в словаре';
                    warningDiv.style.display = 'block';
                    saveButton.disabled = false; // Разрешаем сохранение дубликатов
                } else {
                    checkSaveButtonState();
                }
            })
            .catch(error => {
                warningDiv.className = 'alert alert-danger';
                warningDiv.textContent = 'Ошибка при проверке дубликатов';
                warningDiv.style.display = 'block';
                saveButton.disabled = true;
            });
    }, 300);
}

function saveCustomWord() {
    console.log('=== saveCustomWord START ===');
    
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        return;
    }

    const word = document.getElementById('customWord').value.trim();
    const translation = document.getElementById('customTranslation').value.trim();
    const exampleSentence = document.getElementById('customExample').value.trim();
    const exampleTranslation = document.getElementById('customExampleTranslation').value.trim();
    const warningDiv = document.getElementById('customWordWarning');

    console.log('Saving custom word:', { word, translation, exampleSentence, exampleTranslation, userId: currentUserId });

    if (!word || !translation) {
        console.error('Validation failed: word or translation is empty');
        warningDiv.className = 'alert alert-danger';
        warningDiv.textContent = 'Слово и перевод обязательны для заполнения';
        warningDiv.style.display = 'block';
        return;
    }

    // Создаем FormData для отправки как form-data
    const formData = new FormData();
    formData.append('word', word);
    formData.append('translation', translation);
    formData.append('example', exampleSentence);
    formData.append('exampleTranslation', exampleTranslation);
    formData.append('userId', currentUserId);

    console.log('Sending POST request to /api/dashboard/words/custom/add');

    fetch('/api/dashboard/words/custom/add', {
        method: 'POST',
        credentials: 'include',
        body: formData
    })
        .then(response => {
            console.log('Save custom word response status:', response.status);
            if (response.ok) {
                console.log('Custom word saved successfully');
                hideAddCustomWordForm();
                loadCustomWords();
                updateDashboardCounts();
                warningDiv.style.display = 'none';
            } else {
                throw new Error('Ошибка при сохранении слова');
            }
        })
        .catch(error => {
            console.error('Error saving custom word:', error);
            warningDiv.className = 'alert alert-danger';
            warningDiv.textContent = 'Ошибка при сохранении слова: ' + error.message;
            warningDiv.style.display = 'block';
        })
        .finally(() => {
            console.log('=== saveCustomWord END ===');
        });
}

function checkCustomWord() {
    checkAutocomplete();
}

document.addEventListener('DOMContentLoaded', function() {
    // Загружаем количество слов из текстов
    fetchTextWordsCount();
    
    // Исправление проблемы с aria-hidden и фокусом при закрытии модальных окон
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        // Обработчик закрытия модального окна
        modal.addEventListener('hidden.bs.modal', function () {
            if (document.activeElement && modal.contains(document.activeElement)) {
                document.activeElement.blur();
            }
            setTimeout(() => {
                document.body.focus();
            }, 10);
        });

        // Обработчик показа модального окна
        modal.addEventListener('show.bs.modal', function () {
            if (this.id === 'customWordsModal') {
                loadCustomWords();
            }
        });
    });
});