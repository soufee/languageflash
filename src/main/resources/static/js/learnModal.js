let learnWords = [];
let sessionWords = []; // Список слов для текущей сессии
let showTranslation = false;
let showExample = false;

function loadLearnWords(callback) {
    fetch('/dashboard/active-words-json')
        .then(response => response.json())
        .then(data => {
            learnWords = data;
            console.log('Loaded learnWords:', learnWords);
            if (callback) callback();
        })
        .catch(error => {
            console.error('Error loading learnWords:', error);
            document.getElementById('cardContainer').innerHTML = '<p>Ошибка загрузки слов.</p>';
        });
}

function startCardSession(words) {
    sessionWords = [...words]; // Копируем слова для сессии
    shuffleArray(sessionWords);
    console.log('Started card session with words:', sessionWords);
    showNextCard();
}

function showNextCard() {
    if (sessionWords.length === 0) {
        document.getElementById('cardContainer').innerHTML = `
            <p>Все слова пройдены!</p>
            <button class="btn btn-primary" onclick="endCardSession(false)">Закончить</button>
            <button class="btn btn-secondary" onclick="endCardSession(true)">Продолжить</button>
        `;
        return;
    }

    const word = sessionWords.shift(); // Удаляем и берём первое слово
    document.getElementById('cardWord').textContent = word.word;
    document.getElementById('cardTranslation').textContent = word.translation; // Убрали "Перевод:"
    document.getElementById('cardExample').textContent = word.exampleSentence || ''; // Убрали "Пример:"
    document.getElementById('cardExampleTranslation').textContent = word.exampleTranslation || ''; // Убрали "Перевод примера:"
    document.getElementById('wordId').value = word.id;

    showTranslation = false;
    showExample = false;
    document.getElementById('cardTranslation').style.display = 'none';
    document.getElementById('cardExample').style.display = 'none';
    document.getElementById('cardExampleTranslation').style.display = 'none';
}

function endCardSession(continueSession) {
    if (continueSession) {
        startCardSession(learnWords); // Перезапускаем с полным списком
    } else {
        const learnModal = bootstrap.Modal.getInstance(document.getElementById('learnModal'));
        learnModal.hide();
    }
}

function submitLearnForm(knows) {
    const wordId = document.getElementById('wordId').value;
    fetch('/learn/update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `wordId=${wordId}&knows=${knows}`
    })
        .then(response => {
            if (!response.ok) throw new Error('Ошибка при обновлении прогресса');
            const word = learnWords.find(w => w.id === parseInt(wordId));
            if (knows) {
                word.knowledgeFactor *= 0.75;
                if (word.knowledgeFactor <= 0.1) {
                    word.knowledgeFactor = 0;
                    learnWords = learnWords.filter(w => w.id !== parseInt(wordId));
                }
            } else {
                word.knowledgeFactor = Math.min(word.knowledgeFactor * 1.3, 10.0);
            }
            showNextCard();
            updateDashboardCounts();
        })
        .catch(error => {
            console.error('Ошибка:', error);
            alert('Ошибка при обновлении прогресса');
        });
}

function initLearnModal() {
    const learnModal = document.getElementById('learnModal');
    learnModal.addEventListener('show.bs.modal', function () {
        if (learnWords.length === 0) {
            loadLearnWords(() => {
                startCardSession(learnWords);
            });
        } else {
            startCardSession(learnWords);
        }
    });

    const cardBody = document.getElementById('cardBody');
    cardBody.addEventListener('click', function (event) {
        if (event.target.tagName !== 'BUTTON') {
            if (!showTranslation) {
                showTranslation = true;
                document.getElementById('cardTranslation').style.display = 'block';
            } else if (!showExample) {
                showExample = true;
                document.getElementById('cardExample').style.display = 'block';
                document.getElementById('cardExampleTranslation').style.display = 'block';
            } else if (showTranslation && showExample) {
                showNextCard(); // Переход к следующему слову без обновления прогресса
            }
        }
    });
}