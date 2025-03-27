let currentWords = [];
let currentIndex = 0;
let learnModalInstance = null;
let isTranslationVisible = false;
let isExampleVisible = false;
let isInitialized = false;

function initLearnModal() {
    if (isInitialized) {
        console.log('LearnModal: уже инициализирован, пропускаем повторную инициализацию');
        return;
    }

    // Получаем экземпляр модального окна
    const modalEl = document.getElementById('learnModal');
    if (!modalEl) {
        console.error('LearnModal: элемент #learnModal не найден');
        return;
    }

    // Если модальное окно уже открыто, закрываем его
    const existingModal = bootstrap.Modal.getInstance(modalEl);
    if (existingModal) {
        existingModal.hide();
        // Очищаем стили и классы
        document.body.style.overflow = '';
        document.body.style.paddingRight = '';
        document.body.classList.remove('modal-open');
    }

    // Загружаем слова
    fetch('/dashboard/active-words-json')
        .then(response => {
            if (!response.ok) {
                throw new Error('Ошибка загрузки активных слов: ' + response.status);
            }
            return response.text();
        })
        .then(data => {
            currentWords = JSON.parse(data);
            currentWords = shuffleArray(currentWords);
            currentIndex = 0;
            showCardContent();

            // Создаем новый экземпляр модального окна
            learnModalInstance = new bootstrap.Modal(modalEl, {
                backdrop: true,
                keyboard: true
            });

            // Показываем модальное окно
            learnModalInstance.show();
            isInitialized = true;
        })
        .catch(error => console.error('Ошибка загрузки слов:', error));
}

function initLearnModalWithWords(words) {
    if (isInitialized) {
        console.log('LearnModal: уже инициализирован, пропускаем повторную инициализацию');
        return;
    }

    // Получаем экземпляр модального окна
    const modalEl = document.getElementById('learnModal');
    if (!modalEl) {
        console.error('LearnModal: элемент #learnModal не найден');
        return;
    }

    // Если модальное окно уже открыто, закрываем его
    const existingModal = bootstrap.Modal.getInstance(modalEl);
    if (existingModal) {
        existingModal.hide();
        // Очищаем стили и классы
        document.body.style.overflow = '';
        document.body.style.paddingRight = '';
        document.body.classList.remove('modal-open');
    }

    currentWords = words;
    currentWords = shuffleArray(currentWords);
    currentIndex = 0;
    showCardContent();

    // Создаем новый экземпляр модального окна
    learnModalInstance = new bootstrap.Modal(modalEl, {
        backdrop: true,
        keyboard: true
    });

    // Показываем модальное окно
    learnModalInstance.show();
    isInitialized = true;
}

function showCardContent() {
    const cardWord = document.getElementById('cardWord');
    const cardTranslation = document.getElementById('cardTranslation');
    const cardExample = document.getElementById('cardExample');
    const cardExampleTranslation = document.getElementById('cardExampleTranslation');
    const learnForm = document.getElementById('learnForm');
    const noWordsMessage = document.getElementById('noWordsMessage');

    if (currentIndex < currentWords.length) {
        const word = currentWords[currentIndex];

        isTranslationVisible = false;
        isExampleVisible = false;

        cardWord.textContent = word.word;
        cardTranslation.textContent = word.translation;
        cardTranslation.style.display = 'none';
        cardExample.textContent = word.exampleSentence || '';
        cardExample.style.display = 'none';
        cardExampleTranslation.textContent = word.exampleTranslation || '';
        cardExampleTranslation.style.display = 'none';
        document.getElementById('wordId').value = word.id;

        const cardContainer = document.getElementById('cardContainer');
        cardContainer.onclick = function() {
            handleCardClick();
        };

        learnForm.style.display = 'inline';
        noWordsMessage.style.display = 'none';
    } else {
        cardWord.textContent = '';
        cardTranslation.textContent = '';
        cardTranslation.style.display = 'none';
        cardExample.textContent = '';
        cardExample.style.display = 'none';
        cardExampleTranslation.textContent = '';
        cardExampleTranslation.style.display = 'none';
        showNoWordsMessage();
    }
}

function handleCardClick() {
    if (!isTranslationVisible) {
        document.getElementById('cardTranslation').style.display = 'block';
        isTranslationVisible = true;
    } else if (!isExampleVisible) {
        document.getElementById('cardExample').style.display = 'block';
        document.getElementById('cardExampleTranslation').style.display = 'block';
        isExampleVisible = true;
    } else {
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
            currentWords = shuffleArray(currentWords); // Перемешиваем новые слова
            currentIndex = 0;
            showCardContent();
        })
        .catch(error => console.error('Ошибка получения новых слов:', error));
}

function getUserId() {
    return window.userId;
}

function openLearnModal() {
    initLearnModal();
}

document.addEventListener('DOMContentLoaded', function () {
    try {
        const modalElement = document.getElementById('learnModal');
        if (modalElement) {
            console.log('LearnModal: инициализация модального окна');
            
            // Удаляем обработчик shown.bs.modal, так как он может вызывать проблемы
            modalElement.addEventListener('hidden.bs.modal', function () {
                isInitialized = false;
                // Очищаем стили и классы при закрытии модального окна
                document.body.style.overflow = '';
                document.body.style.paddingRight = '';
                document.body.classList.remove('modal-open');
            });
        } else {
            console.log("LearnModal: элемент #learnModal не найден в DOM");
        }
    } catch (error) {
        console.error('Ошибка при инициализации learnModal:', error);
    }
});