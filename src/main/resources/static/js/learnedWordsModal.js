let learnedWordsList = [];

function loadLearnedWords(callback) {
    fetch('/learn/learned-words')
        .then(response => response.json())
        .then(data => {
            learnedWordsList = data;
            console.log('Loaded learnedWordsList:', learnedWordsList);
            if (callback) callback();
        })
        .catch(error => {
            console.error('Error loading learnedWordsList:', error);
            document.getElementById('learnedWordsTable').innerHTML = '<p>Ошибка загрузки слов.</p>';
        });
}

function updateLearnedWordsTable() {
    const tbody = document.getElementById('learnedWordsBody');
    const noWordsMessage = document.getElementById('noLearnedWords');
    const reviewButton = document.getElementById('reviewLearnedButton');
    tbody.innerHTML = '';
    if (learnedWordsList.length === 0) {
        noWordsMessage.style.display = 'block';
        reviewButton.disabled = true;
    } else {
        noWordsMessage.style.display = 'none';
        reviewButton.disabled = false;
        learnedWordsList.forEach(word => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${word.word}</td>
                <td>${word.translation}</td>
            `;
            tbody.appendChild(tr);
        });
    }
}

function reviewLearnedWords() {
    const learnModal = new bootstrap.Modal(document.getElementById('learnModal'));
    learnWords = [...learnedWordsList];
    startCardSession(learnWords);
}

function initLearnedWordsModal() {
    const learnedWordsModal = document.getElementById('learnedWordsModal');
    learnedWordsModal.addEventListener('show.bs.modal', function () {
        if (learnedWordsList.length === 0) {
            loadLearnedWords(updateLearnedWordsTable);
        } else {
            updateLearnedWordsTable();
        }
    });
}