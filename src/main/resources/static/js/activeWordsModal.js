let activeWordsList = [];
let selectedAdditionalTags = new Set();

function loadActiveWords(callback) {
    fetch('/dashboard/active-words-json')
        .then(response => response.json())
        .then(data => {
            activeWordsList = data;
            console.log('Loaded activeWordsList:', activeWordsList);
            if (callback) callback();
        })
        .catch(error => {
            console.error('Error loading activeWordsList:', error);
            document.getElementById('activeWordsTable').innerHTML = '<p>Ошибка загрузки слов.</p>';
        });
}

function updateActiveWordsTable() {
    const tbody = document.getElementById('activeWordsBody');
    const noWordsMessage = document.getElementById('noActiveWords');
    const refillButton = document.getElementById('refillButton');
    tbody.innerHTML = '';
    if (activeWordsList.length === 0) {
        noWordsMessage.style.display = 'block';
        refillButton.disabled = false;
    } else {
        noWordsMessage.style.display = 'none';
        activeWordsList.forEach(word => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${word.word}</td>
                <td>${word.translation}</td>
                <td><button type="button" class="btn btn-danger btn-sm" onclick="markAsLearned(${word.id})"><i class="fas fa-trash"></i></button></td>
            `;
            tbody.appendChild(tr);
        });
        getActiveWordsCount().then(activeWordsCount => {
            refillButton.disabled = activeWordsList.length >= activeWordsCount;
            console.log('Refill button state:', refillButton.disabled, 'Current:', activeWordsList.length, 'Target:', activeWordsCount);
        });
    }
}

function markAsLearned(wordId) {
    fetch('/learn/update', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: `wordId=${wordId}&knows=true&forceLearned=true`
    })
        .then(response => {
            if (!response.ok) throw new Error('Ошибка при перемещении слова');
            activeWordsList = activeWordsList.filter(word => word.id !== wordId);
            learnWords = activeWordsList; // Обновляем learnWords для карточек
            updateActiveWordsTable();
            updateDashboardCounts();
        })
        .catch(error => {
            console.error('Ошибка:', error);
            alert('Ошибка при перемещении слова');
        });
}

function refillActiveWords() {
    getActiveWordsCount()
        .then(activeWordsCount => {
            console.log('Current active words:', activeWordsList.length, 'Target:', activeWordsCount);
            if (activeWordsList.length < activeWordsCount) {
                fetch('/learn/refill', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    }
                })
                    .then(response => {
                        if (!response.ok) throw new Error('Ошибка при пополнении слов');
                        return response.json();
                    })
                    .then(data => {
                        activeWordsList = data.activeWords;
                        learnWords = activeWordsList; // Обновляем learnWords для карточек
                        document.getElementById('activeWordsCount').textContent = data.activeCount;
                        document.getElementById('learnedWordsCount').textContent = data.learnedCount;
                        console.log('Refilled activeWordsList:', activeWordsList);
                        console.log('Updated dashboard counts - Active:', data.activeCount, 'Learned:', data.learnedCount);
                        updateActiveWordsTable();
                        if (data.showTagPrompt) {
                            const addTagsModal = new bootstrap.Modal(document.getElementById('addTagsModal'));
                            const modalBody = document.getElementById('addTagsModal').querySelector('.modal-body');
                            const existingPrompt = modalBody.querySelector('.prompt-message');
                            if (existingPrompt) existingPrompt.remove();
                            const prompt = document.createElement('p');
                            prompt.className = 'prompt-message';
                            prompt.innerHTML = `Для вашей программы найдено только ${data.activeCount} слов. Чтобы начать обучение, нужно как минимум ${activeWordsCount} слов. Пожалуйста, выберите дополнительные темы:`;
                            modalBody.insertBefore(prompt, modalBody.querySelector('form'));
                            selectedAdditionalTags.clear();
                            modalBody.querySelectorAll('.tag-card').forEach(card => card.classList.remove('selected'));
                            const form = document.getElementById('addTagsForm');
                            form.onsubmit = function(event) {
                                event.preventDefault();
                                fetch('/dashboard/add-tags', {
                                    method: 'POST',
                                    headers: {
                                        'Content-Type': 'application/x-www-form-urlencoded'
                                    },
                                    body: `tags=${Array.from(selectedAdditionalTags).join(',')}`
                                })
                                    .then(response => {
                                        if (!response.ok) throw new Error('Ошибка при добавлении тегов');
                                        addTagsModal.hide();
                                        refillActiveWords();
                                    })
                                    .catch(error => console.error('Error adding tags:', error));
                            };
                            addTagsModal.show();
                        }
                    })
                    .catch(error => console.error('Error refilling active words:', error));
            } else {
                console.log('No refill needed, active words count is sufficient');
            }
        });
}

function toggleAdditionalTag(card) {
    const tag = card.getAttribute('data-tag');
    if (selectedAdditionalTags.has(tag)) {
        selectedAdditionalTags.delete(tag);
        card.classList.remove('selected');
    } else {
        selectedAdditionalTags.add(tag);
        card.classList.add('selected');
    }
    document.getElementById('additionalTags').value = Array.from(selectedAdditionalTags).join(',');
}

function initActiveWordsModal() {
    const activeWordsModal = document.getElementById('activeWordsModal');
    activeWordsModal.addEventListener('show.bs.modal', function () {
        if (activeWordsList.length === 0) {
            loadActiveWords(updateActiveWordsTable);
        } else {
            updateActiveWordsTable();
        }
    });

    const addTagsLink = document.querySelector('a[data-bs-target="#addTagsModal"]');
    if (addTagsLink) {
        addTagsLink.addEventListener('click', function(event) {
            event.preventDefault();
            const addTagsModal = new bootstrap.Modal(document.getElementById('addTagsModal'));
            const modalBody = document.getElementById('addTagsModal').querySelector('.modal-body');
            const existingPrompt = modalBody.querySelector('.prompt-message');
            if (existingPrompt) existingPrompt.remove();
            selectedAdditionalTags.clear();
            modalBody.querySelectorAll('.tag-card').forEach(card => card.classList.remove('selected'));
            addTagsModal.show();
        });
    }
}