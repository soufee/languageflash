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

            if (words.length === 0) {
                noWordsMessage.style.display = 'block';
                refillButton.disabled = false;
            } else {
                noWordsMessage.style.display = 'none';
                refillButton.disabled = true;

                words.forEach(word => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${word.word}</td>
                        <td>${word.translation}</td>
                        <td><button class="btn btn-sm btn-danger" onclick="removeWord(${word.id})">Удалить</button></td>
                    `;
                    tbody.appendChild(row);
                });
            }
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

document.addEventListener('DOMContentLoaded', function () {
    const modalElement = document.getElementById('activeWordsModal');
    if (modalElement) {
        modalElement.addEventListener('shown.bs.modal', function () {
            loadActiveWords();
        });
    } else {
        console.error("Элемент #activeWordsModal не найден в DOM");
    }
});