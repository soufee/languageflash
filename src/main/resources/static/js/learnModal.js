let currentWords = [];
let currentIndex = 0;
let learnModalInstance = null;
let isTranslationVisible = false;
let isExampleVisible = false;

function initLearnModal() {
    fetch('/dashboard/active-words-json')
        .then(response => {
            if (!response.ok) {
                throw new Error('Ошибка загрузки активных слов: ' + response.status);
            }
            return response.text();
        })
        .then(data => {
            currentWords = JSON.parse(data);
            currentIndex = 0;
            showCardContent();

            const modalEl = document.getElementById('learnModal');
            const existingInstance = bootstrap.Modal.getInstance(modalEl);

            if (existingInstance) {
                learnModalInstance = existingInstance;
            } else {
                learnModalInstance = new bootstrap.Modal(modalEl, {
                    backdrop: true,
                    keyboard: true
                });
            }

            learnModalInstance.show();
        })
        .catch(error => console.error('Ошибка загрузки слов:', error));
}

function showCardContent() {
    if (currentIndex < currentWords.length) {
        const word = currentWords[currentIndex];

        // Сбрасываем состояние видимости для нового слова
        isTranslationVisible = false;
        isExampleVisible = false;

        document.getElementById('cardWord').textContent = word.word;
        document.getElementById('cardTranslation').textContent = word.translation;
        document.getElementById('cardTranslation').style.display = 'none';
        document.getElementById('cardExample').textContent = word.exampleSentence || '';
        document.getElementById('cardExample').style.display = 'none';
        document.getElementById('cardExampleTranslation').textContent = word.exampleTranslation || '';
        document.getElementById('cardExampleTranslation').style.display = 'none';
        document.getElementById('wordId').value = word.id;

        // Устанавливаем обработчик для всего контейнера карточки
        const cardContainer = document.getElementById('cardContainer');
        cardContainer.onclick = function() {
            handleCardClick();
        };

        document.getElementById('learnForm').style.display = 'inline';
        document.getElementById('noWordsMessage').style.display = 'none';
    } else {
        showNoWordsMessage();
    }
}

function handleCardClick() {
    if (!isTranslationVisible) {
        // Показываем перевод при первом клике
        document.getElementById('cardTranslation').style.display = 'block';
        isTranslationVisible = true;
    } else if (!isExampleVisible) {
        // Показываем примеры при втором клике
        document.getElementById('cardExample').style.display = 'block';
        document.getElementById('cardExampleTranslation').style.display = 'block';
        isExampleVisible = true;
    } else {
        // При третьем клике переходим к следующему слову
        currentIndex++;
        showCardContent();
    }
}

function showNoWordsMessage() {
    document.getElementById('learnForm').style.display = 'none';
    document.getElementById('noWordsMessage').style.display = 'block';
}

function submitLearnForm(knows) {
    const wordId = document.getElementById('wordId').value;
    fetch('/dashboard/update-progress', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ userId: getUserId(), wordId: wordId, knows: knows })
    })
        .then(response => response.json())
        .then(data => {
            currentIndex++;
            showCardContent();
        })
        .catch(error => console.error('Ошибка обновления прогресса:', error));
}

function refillWords() {
    fetch('/dashboard/refill-words', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ userId: getUserId() })
    })
        .then(response => response.json())
        .then(data => {
            currentWords = data;
            currentIndex = 0;
            showCardContent();
        })
        .catch(error => console.error('Ошибка получения новых слов:', error));
}

function getUserId() {
    return window.userId || 1; // Замените на вашу логику получения userId
}

function openLearnModal() {
    initLearnModal();
}

document.addEventListener('DOMContentLoaded', function () {
    const modalEl = document.getElementById('learnModal');
    if (modalEl) {
        modalEl.addEventListener('hidden.bs.modal', function () {
            console.log('Modal closed, cleaning up...');
            const body = document.body;
            body.classList.remove('modal-open');
            body.style.overflow = '';
            body.style.paddingRight = '';
            const backdrops = document.getElementsByClassName('modal-backdrop');
            for (let i = backdrops.length - 1; i >= 0; i--) {
                backdrops[i].remove();
            }
            window.dispatchEvent(new Event('resize'));
        });
    } else {
        console.error("Элемент #learnModal не найден в DOM");
    }
});