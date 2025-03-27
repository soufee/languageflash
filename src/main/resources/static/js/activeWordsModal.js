function loadActiveWords() {
    fetch('/dashboard/active-words-json')
        .then(response => {
            if (!response.ok) {
                throw new Error('Ошибка загрузки активных слов: ' + response.status);
            }
            return response.text();
        })
        .then(data => {
            const words = JSON.parse(data);
            const tbody = document.getElementById('activeWordsBody');
            const noWordsMessage = document.getElementById('noActiveWords');
            const refillButton = document.getElementById('refillButton');

            tbody.innerHTML = '';

            // Получаем лимит активных слов из настроек
            getActiveWordsCount()
                .then(activeWordsCount => {
                    if (words.length === 0) {
                        noWordsMessage.style.display = 'block';
                        refillButton.disabled = false;
                    } else {
                        noWordsMessage.style.display = 'none';
                        // Кнопка активна, если слов меньше лимита
                        refillButton.disabled = words.length >= activeWordsCount;
                    }

                    words.forEach(word => {
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
                        refillButton.disabled = false;
                    } else {
                        noWordsMessage.style.display = 'none';
                        refillButton.disabled = true;
                    }
                });
        })
        .catch(error => {
            console.error('Ошибка:', error);
            document.getElementById('activeWordsBody').innerHTML = '<tr><td colspan="3">Ошибка загрузки слов</td></tr>';
        });
}

function removeWord(wordId) {
    fetch('/dashboard/remove-word', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ wordId: wordId })
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Ошибка удаления слова: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            if (data.status === 'success') {
                loadActiveWords(); // Обновляем список активных слов
                updateDashboardCounts(); // Обновляем счетчики на дашборде
            }
        })
        .catch(error => console.error('Ошибка удаления слова:', error));
}

function refillActiveWords() {
    fetch('/learn/refill', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Ошибка подгрузки слов: ' + response.status);
            }
            return response.json();
        })
        .then(data => {
            loadActiveWords(); // Обновляем список после подгрузки
            updateDashboardCounts(); // Обновляем счетчики
        })
        .catch(error => console.error('Ошибка подгрузки слов:', error));
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