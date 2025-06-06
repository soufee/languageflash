let currentWords = [];
let currentIndex = 0;
let learnModalInstance = null;
let isTranslationVisible = false;
let isExampleVisible = false;
let isInitialized = false;

function initLearnModal() {
    console.log('=== initLearnModal START ===');
    
    if (isInitialized) {
        console.log('LearnModal: уже инициализирован, пропускаем повторную инициализацию');
        return;
    }

    // Check if currentUserId is defined
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        alert('Ошибка: ID пользователя не найден. Невозможно открыть карточки.');
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
    const url = `/api/dashboard/words/active?userId=${currentUserId}`;
    console.log('Loading active words from:', url);
    
    fetch(url, {
        method: 'GET',
        credentials: 'include'
    })
        .then(response => {
            console.log('Active words response status:', response.status);
            if (!response.ok) {
                throw new Error('Ошибка загрузки активных слов: ' + response.status);
            }
            return response.json(); // Используем response.json() вместо response.text()
        })
        .then(wordProgressList => {
            console.log('Received word progress list:', wordProgressList);
            // Преобразуем WordProgress в обычные объекты слов для совместимости
            currentWords = wordProgressList.map(wp => ({
                id: wp.word.id,
                word: wp.word.word,
                translation: wp.word.translation,
                exampleSentence: wp.word.exampleSentence,
                exampleTranslation: wp.word.exampleTranslation
            }));
            console.log('Processed words for learn modal:', currentWords);
            
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
            console.log('Learn modal initialized and shown');
        })
        .catch(error => {
            console.error('Ошибка загрузки слов:', error);
            alert('Ошибка при загрузке слов для карточек: ' + error.message);
        });
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
    console.log('=== submitLearnForm called with knows:', knows, '===');
    
    const wordId = document.getElementById('wordId').value;
    console.log('Updating progress for wordId:', wordId, 'knows:', knows);
    
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        return;
    }
    
    const url = `/api/dashboard/words/progress?userId=${currentUserId}&wordId=${wordId}&knows=${knows}`;
    console.log('Submitting progress to:', url);
    
    fetch(url, {
        method: 'POST',
        credentials: 'include'
    })
        .then(response => {
            console.log('Progress update response status:', response.status);
            if (response.ok) {
                currentIndex++;
                showCardContent();
            } else {
                throw new Error('Ошибка обновления прогресса: ' + response.status);
            }
        })
        .catch(error => {
            console.error('Ошибка обновления прогресса:', error);
            alert('Ошибка при обновлении прогресса: ' + error.message);
        });
}

function refillWords() {
    console.log('=== refillWords called ===');
    
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        return;
    }
    
    // Получаем настройки пользователя для подгрузки слов
    fetch(`/api/dashboard/settings?userId=${currentUserId}`, {
        credentials: 'include'
    })
        .then(response => {
            if (!response.ok) {
                throw new Error('Ошибка получения настроек: ' + response.status);
            }
            return response.json();
        })
        .then(settings => {
            console.log('User settings for refill:', settings);
            
            const language = settings.language;
            const minLevel = settings.minLevel;
            const tags = settings.tags || [];
            
            if (!language || !minLevel) {
                alert('Для пополнения слов необходимо настроить программу обучения');
                return;
            }
            
            const url = `/api/dashboard/words/refill?userId=${currentUserId}&language=${encodeURIComponent(language)}&minLevel=${encodeURIComponent(minLevel)}${tags.length > 0 ? '&tags=' + tags.map(encodeURIComponent).join('&tags=') : ''}`;
            console.log('Refilling words via:', url);
            
            return fetch(url, {
                method: 'POST',
                credentials: 'include'
            });
        })
        .then(response => {
            console.log('Refill response status:', response.status);
            if (!response.ok) {
                throw new Error('Ошибка подгрузки слов: ' + response.status);
            }
            console.log('Words refilled successfully, reloading learn modal');
            // Перезагружаем модальное окно с новыми словами
            isInitialized = false;
            initLearnModal();
        })
        .catch(error => {
            console.error('Ошибка получения новых слов:', error);
            alert('Ошибка при получении новых слов: ' + error.message);
        });
}

function getUserId() {
    return currentUserId || window.currentUserId || window.userId;
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