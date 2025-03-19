function updateDashboardCounts() {
    fetch('/dashboard/active-words-json')
        .then(response => response.text())
        .then(data => {
            const activeWords = JSON.parse(data);
            const activeCount = activeWords.length;
            document.getElementById('activeWordsCount').textContent = activeCount;
            console.log('Updated active words count:', activeCount);

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
                    console.log('Updated learned words count:', learnedCount);

                    fetch('/dashboard/custom-words')
                        .then(response => response.json())
                        .then(customWords => {
                            document.getElementById('customWordsCount').textContent = customWords.length;
                            console.log('Updated custom words count:', customWords.length);
                        });
                })
                .catch(error => console.error('Ошибка загрузки выученных слов:', error));
        })
        .catch(error => console.error('Ошибка загрузки активных слов:', error));
}

function loadCustomWords() {
    fetch('/dashboard/custom-words')
        .then(response => {
            if (!response.ok) {
                throw new Error('Ошибка загрузки кастомных слов: ' + response.status);
            }
            return response.json();
        })
        .then(words => {
            const tbody = document.getElementById('customWordsBody');
            const noWordsMessage = document.getElementById('noCustomWords');
            tbody.innerHTML = '';

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
            console.error('Ошибка:', error);
            document.getElementById('customWordsBody').innerHTML = '<tr><td colspan="2">Ошибка загрузки слов</td></tr>';
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
}

function hideAddCustomWordForm() {
    document.getElementById('addCustomWordForm').style.display = 'none';
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

    fetch('/dashboard/custom-words/check-autocomplete', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ word: word })
    })
        .then(response => response.json())
        .then(data => {
            console.log('Autocomplete response:', data);
            if (data.status === 'autocomplete') {
                document.getElementById('customTranslation').value = data.translation || '';
                document.getElementById('customExample').value = data.exampleSentence || '';
                document.getElementById('customExampleTranslation').value = data.exampleTranslation || '';
                saveButton.disabled = !document.getElementById('customTranslation').value.trim();
            }
        })
        .catch(error => {
            console.error('Ошибка автокомплита:', error);
        });
}

function checkDuplicates() {
    const word = document.getElementById('customWord').value.trim();
    const translation = document.getElementById('customTranslation').value.trim();
    const saveButton = document.getElementById('saveCustomWordButton');
    const warningDiv = document.getElementById('customWordWarning');

    warningDiv.style.display = 'none';
    saveButton.disabled = true;

    if (!word || !translation) {
        return;
    }

    fetch('/dashboard/custom-words/check-duplicates', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ word: word, translation: translation })
    })
        .then(response => response.json())
        .then(data => {
            console.log('Duplicates response:', data);
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
                saveButton.disabled = false;
            }
        })
        .catch(error => {
            console.error('Ошибка проверки дубликатов:', error);
            warningDiv.className = 'alert alert-danger';
            warningDiv.textContent = 'Ошибка при проверке дубликатов';
            warningDiv.style.display = 'block';
            saveButton.disabled = true;
        });
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

document.addEventListener('DOMContentLoaded', function () {
    initProgramModal();
    initFlashLearnModal();
    const showTagPrompt = /*[[${showTagPrompt != null ? showTagPrompt : false}]]*/ false;
    if (showTagPrompt) {
        const addTagsModal = new bootstrap.Modal(document.getElementById('addTagsModal'));
        const modalBody = document.getElementById('addTagsModal').querySelector('.modal-body');
        const activeWordsCount = /*[[${progressCount}]]*/ 0;
        const targetWordsCount = /*[[${settings['activeWordsCount'] ?: 50}]]*/ 50;
        const prompt = document.createElement('p');
        prompt.className = 'prompt-message';
        prompt.innerHTML = `Для вашей программы найдено только ${activeWordsCount} слов. Чтобы начать обучение, нужно как минимум ${targetWordsCount} слов. Пожалуйста, выберите дополнительные темы:`;
        modalBody.insertBefore(prompt, modalBody.querySelector('form'));
        addTagsModal.show();
    }

    const customWordsModal = document.getElementById('customWordsModal');
    if (customWordsModal) {
        customWordsModal.addEventListener('shown.bs.modal', function () {
            loadCustomWords();
        });
    }

    // Добавляем слушатели blur
    document.getElementById('customWord').addEventListener('blur', checkAutocomplete);
    document.getElementById('customTranslation').addEventListener('blur', checkDuplicates);
});