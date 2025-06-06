let flashInterval;
let flashTimerInterval;
let remainingTime;
let flashWords = [];

function loadFlashWords(callback) {
    console.log('=== loadFlashWords START ===');
    
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        document.getElementById('flashSettings').innerHTML = '<p class="text-danger">Ошибка: ID пользователя не найден.</p>';
        return;
    }
    
    const url = `/api/dashboard/words/active?userId=${currentUserId}`;
    console.log('Loading flash words from:', url);
    
    fetch(url, {
        method: 'GET',
        credentials: 'include'
    })
        .then(response => {
            console.log('Flash words response status:', response.status);
            if (!response.ok) {
                throw new Error('Ошибка загрузки активных слов: ' + response.status);
            }
            return response.json();
        })
        .then(wordProgressList => {
            console.log('Received word progress list for flash:', wordProgressList);
            // Преобразуем WordProgress в обычные объекты слов для совместимости
            flashWords = wordProgressList.map(wp => ({
                id: wp.word.id,
                word: wp.word.word,
                translation: wp.word.translation,
                knowledgeFactor: wp.knowledgeFactor || 1.0
            }));
            console.log('Processed flash words:', flashWords);
            
            if (callback) callback();
        })
        .catch(error => {
            console.error('Error loading flashWords:', error);
            document.getElementById('flashSettings').innerHTML = '<p class="text-danger">Ошибка загрузки слов: ' + error.message + '</p>';
        });
}

function startFlashSession() {
    const speed = parseInt(document.getElementById('flashSpeed').value);
    const duration = parseInt(document.getElementById('flashDuration').value) * 60;
    remainingTime = duration;

    document.getElementById('flashSettings').style.display = 'none';
    document.getElementById('flashDisplay').style.display = 'block';

    if (!flashWords || flashWords.length === 0) {
        stopFlashSession();
        return;
    }

    document.getElementById('flashTimer').textContent = formatTime(remainingTime);
    flashTimerInterval = setInterval(() => {
        remainingTime--;
        document.getElementById('flashTimer').textContent = formatTime(remainingTime);
        if (remainingTime <= 0) {
            stopFlashSession(true);
        }
    }, 1000);

    flashInterval = setInterval(() => {
        const word = getRandomWord(flashWords);
        const flashWord = document.getElementById('flashWord');
        const flashTranslation = document.getElementById('flashTranslation');
        const isForward = Math.random() < 0.5;
        const combinedText = isForward ? `${word.word} - ${word.translation}` : `${word.translation} - ${word.word}`;

        if (combinedText.length > 50) {
            flashWord.textContent = isForward ? word.word : word.translation;
            flashTranslation.textContent = isForward ? word.translation : word.word;
            flashTranslation.style.display = 'block';
        } else {
            flashWord.textContent = combinedText;
            flashTranslation.textContent = '';
            flashTranslation.style.display = 'none';
        }

        const angle = Math.random() * 90 - 45;
        flashWord.style.transform = `rotate(${angle}deg)`;
        if (flashTranslation.style.display !== 'none') {
            flashTranslation.style.transform = `rotate(${angle}deg)`;
        }
    }, 1000 / speed);
}

function stopFlashSession(end = false) {
    clearInterval(flashInterval);
    clearInterval(flashTimerInterval);
    document.getElementById('flashDisplay').style.display = 'none';
    if (end) {
        document.getElementById('flashEnd').style.display = 'block';
    } else {
        document.getElementById('flashSettings').style.display = 'block';
    }
}

function openLearnModal() {
    const flashModal = bootstrap.Modal.getInstance(document.getElementById('flashLearnModal'));
    if (flashModal) flashModal.hide();
    
    const learnModal = document.getElementById('learnModal');
    if (learnModal) {
        const learnModalInstance = new bootstrap.Modal(learnModal);
        learnModalInstance.show();
    } else {
        console.warn('learnModal не найден в DOM');
    }
}

function initFlashLearnModal() {
    const flashModal = document.getElementById('flashLearnModal');
    if (!flashModal) {
        console.warn('flashLearnModal не найден в DOM');
        return;
    }
    
    flashModal.addEventListener('show.bs.modal', function () {
        console.log('FlashLearnModal: открытие модального окна');
        loadFlashWords(() => {
            console.log('FlashLearnModal: слова загружены');
            if (flashWords.length === 0) {
                document.getElementById('flashSettings').innerHTML = '<p class="text-danger">Нет слов для флеш-запоминания.</p>';
            }
        });
    });
}

// Обертка вызова инициализации через modalWrapper
document.addEventListener('DOMContentLoaded', function() {
    try {
        initFlashLearnModal();
    } catch (error) {
        console.error('Ошибка при инициализации flashLearnModal:', error);
    }
});