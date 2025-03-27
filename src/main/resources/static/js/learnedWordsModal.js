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
    // Закрываем текущее модальное окно
    const learnedWordsModal = bootstrap.Modal.getInstance(document.getElementById('learnedWordsModal'));
    learnedWordsModal.hide();

    // Инициализируем модальное окно с выученными словами
    initLearnModalWithWords(learnedWordsList);

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
    try {
        const modalElement = document.getElementById('learnedWordsModal');
        if (modalElement) {
            console.log('LearnedWordsModal: инициализация модального окна');
            modalElement.addEventListener('shown.bs.modal', function () {
                loadLearnedWords();
            });
        } else {
            console.log("LearnedWordsModal: элемент #learnedWordsModal не найден в DOM");
        }
    } catch (error) {
        console.error('Ошибка при инициализации learnedWordsModal:', error);
    }
});