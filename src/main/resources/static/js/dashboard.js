// Функция для получения количества слов из текстов
function fetchTextWordsCount() {
    fetch('/dashboard/text-words-count')
        .then(response => {
            if (!response.ok) {
                if (response.status === 401) {
                    return { count: 0 };
                }
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            const countElement = document.getElementById('textWordsCount');
            if (countElement) {
                countElement.textContent = data.count;
            }
        })
        .catch(error => {
            const countElement = document.getElementById('textWordsCount');
            if (countElement) {
                countElement.textContent = '0';
            }
        });
}

function updateDashboardCounts() {
    fetch('/dashboard/active-words-json')
        .then(response => response.text())
        .then(data => {
            const activeWords = JSON.parse(data);
            const activeCount = activeWords.length;
            document.getElementById('activeWordsCount').textContent = activeCount;

            fetch('/dashboard/learned-words-json', {
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

                    fetch('/dashboard/custom-words')
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
    
    fetch('/dashboard/custom-words')
        .then(response => {
            if (!response.ok) {
                throw new Error('Ошибка загрузки кастомных слов: ' + response.status);
            }
            return response.json();
        })
        .then(words => {
            if (words.length === 0) {
                noWordsMessage.style.display = 'block';
            } else {
                noWordsMessage.style.display = 'none';
                words.forEach(word => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${word.word}</td>
                        <td>${word.translation}</td>
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
        fetch('/dashboard/custom-words/check-autocomplete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ word: word })
        })
            .then(response => response.json())
            .then(data => {
                if (data.status === 'autocomplete') {
                    document.getElementById('customTranslation').value = data.translation || '';
                    document.getElementById('customExample').value = data.exampleSentence || '';
                    document.getElementById('customExampleTranslation').value = data.exampleTranslation || '';
                    checkSaveButtonState();
                }
            })
            .catch(error => {});
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
        fetch('/dashboard/custom-words/check-duplicates', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ word: word, translation: translation })
        })
            .then(response => response.json())
            .then(data => {
                if (data.status === 'error') {
                    warningDiv.className = 'alert alert-danger';
                    warningDiv.textContent = data.message;
                    warningDiv.style.display = 'block';
                    saveButton.disabled = true;
                } else if (data.status === 'warning') {
                    warningDiv.className = 'alert alert-warning';
                    warningDiv.textContent = data.message;
                    warningDiv.style.display = 'block';
                    saveButton.disabled = false;
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
    const word = document.getElementById('customWord').value.trim();
    const translation = document.getElementById('customTranslation').value.trim();
    const exampleSentence = document.getElementById('customExample').value.trim();
    const exampleTranslation = document.getElementById('customExampleTranslation').value.trim();
    const warningDiv = document.getElementById('customWordWarning');

    if (!word || !translation) {
        warningDiv.className = 'alert alert-danger';
        warningDiv.textContent = 'Слово и перевод обязательны для заполнения';
        warningDiv.style.display = 'block';
        return;
    }

    fetch('/dashboard/custom-words/add', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            word: word,
            translation: translation,
            exampleSentence: exampleSentence,
            exampleTranslation: exampleTranslation,
            force: true
        })
    })
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success') {
                hideAddCustomWordForm();
                loadCustomWords();
                updateDashboardCounts();
            } else if (data.status === 'warning') {
                if (confirm(data.message)) {
                    fetch('/dashboard/custom-words/add', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify({
                            word: word,
                            translation: translation,
                            exampleSentence: exampleSentence,
                            exampleTranslation: exampleTranslation,
                            force: true
                        })
                    })
                        .then(response => response.json())
                        .then(data => {
                            if (data.status === 'success') {
                                hideAddCustomWordForm();
                                loadCustomWords();
                                updateDashboardCounts();
                            }
                        });
                }
            } else if (data.status === 'error') {
                warningDiv.className = 'alert alert-danger';
                warningDiv.textContent = data.message;
                warningDiv.style.display = 'block';
            }
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