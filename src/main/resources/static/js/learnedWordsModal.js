let learnedWordsList = [];

function loadLearnedWords() {
    console.log('=== loadLearnedWords START ===');
    
    // Check if currentUserId is defined
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        document.getElementById('learnedWordsBody').innerHTML = '<tr><td colspan="2">Ошибка: ID пользователя не найден</td></tr>';
        return;
    }
    
    const url = `/api/dashboard/words/learned?userId=${currentUserId}`;
    console.log('Fetching learned words from:', url);
    
    fetch(url, {
        method: 'GET',
        credentials: 'include'
    })
        .then(response => {
            console.log('Response status:', response.status);
            if (!response.ok) {
                throw new Error('Ошибка загрузки выученных слов: ' + response.status);
            }
            return response.json(); // Используем response.json() вместо response.text()
        })
        .then(words => {
            console.log('Learned words received:', words);
            learnedWordsList = words;
            const tbody = document.getElementById('learnedWordsBody');
            const noWordsMessage = document.getElementById('noLearnedWords');

            tbody.innerHTML = '';

            if (learnedWordsList.length === 0) {
                noWordsMessage.style.display = 'block';
            } else {
                noWordsMessage.style.display = 'none';
                learnedWordsList.forEach(wordProgress => {
                    const word = wordProgress.word; // WordProgress содержит объект word
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
    console.log('=== reviewLearnedWords called ===');
    
    if (learnedWordsList.length === 0) {
        alert('Нет выученных слов для повторения');
        return;
    }
    
    // Закрываем текущее модальное окно
    const learnedWordsModal = bootstrap.Modal.getInstance(document.getElementById('learnedWordsModal'));
    if (learnedWordsModal) {
        learnedWordsModal.hide();
    }

    // Инициализируем модальное окно с выученными словами
    if (typeof initLearnModalWithWords === 'function') {
        initLearnModalWithWords(learnedWordsList);
    } else {
        console.error('Function initLearnModalWithWords not found');
        alert('Ошибка: функция повторения не найдена');
        return;
    }

    // Переопределяем submitLearnForm для выученных слов (только переключение)
    const learnFormButtons = document.querySelectorAll('#learnForm button');
    learnFormButtons.forEach(button => {
        button.onclick = function(e) {
            e.preventDefault();
            if (typeof currentIndex !== 'undefined') {
                currentIndex++;
            }
            if (typeof showCardContent === 'function') {
                showCardContent();
            }
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