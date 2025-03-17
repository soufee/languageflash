let learnedWordsList = [];

function loadLearnedWords() {
    fetch('/dashboard/learned-words-json')
        .then(response => {
            if (!response.ok) {
                throw new Error('Ошибка загрузки выученных слов: ' + response.status);
            }
            return response.text();
        })
        .then(data => {
            learnedWordsList = JSON.parse(data);
            const tbody = document.getElementById('learnedWordsBody');
            const noWordsMessage = document.getElementById('noLearnedWords');

            tbody.innerHTML = '';

            if (learnedWordsList.length === 0) {
                noWordsMessage.style.display = 'block';
            } else {
                noWordsMessage.style.display = 'none';
                learnedWordsList.forEach(word => {
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
            document.getElementById('learnedWordsBody').innerHTML = '<tr><td colspan="2">Ошибка загрузки слов</td></tr>';
        });
}

function reviewLearnedWords() {
    currentWords = [...learnedWordsList];
    currentIndex = 0;
    showCardContent();
    const learnModal = new bootstrap.Modal(document.getElementById('learnModal'));
    learnModal.show();

    // Переопределяем submitLearnForm для выученных слов (только переключение)
    document.querySelectorAll('#learnForm button').forEach(button => {
        button.onclick = function(e) {
            e.preventDefault();
            currentIndex++;
            showCardContent();
        };
    });
}

document.addEventListener('DOMContentLoaded', function () {
    const modalElement = document.getElementById('learnedWordsModal');
    if (modalElement) {
        modalElement.addEventListener('shown.bs.modal', function () {
            loadLearnedWords();
        });
    } else {
        console.error("Элемент #learnedWordsModal не найден в DOM");
    }
});