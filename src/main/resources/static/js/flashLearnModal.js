let flashInterval;
let flashTimerInterval;
let remainingTime;
let flashWords = [];

function loadFlashWords(callback) {
    fetch('/dashboard/active-words-json')
        .then(response => response.json())
        .then(data => {
            flashWords = data;
            console.log('Loaded flashWords:', flashWords);
            if (callback) callback();
        })
        .catch(error => {
            console.error('Error loading flashWords:', error);
            document.getElementById('flashSettings').innerHTML = '<p>Ошибка загрузки слов.</p>';
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
    flashModal.hide();
    const learnModal = new bootstrap.Modal(document.getElementById('learnModal'));
    learnModal.show();
}

function initFlashLearnModal() {
    const flashModal = document.getElementById('flashLearnModal');
    flashModal.addEventListener('show.bs.modal', function () {
        if (flashWords.length === 0) {
            loadFlashWords();
        }
    });
}