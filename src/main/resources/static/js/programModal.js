let selectedTags = new Set();

function toggleTag(card) {
    const tag = card.getAttribute('data-tag');
    if (selectedTags.has(tag)) {
        selectedTags.delete(tag);
        card.classList.remove('selected');
    } else {
        selectedTags.add(tag);
        card.classList.add('selected');
    }
    document.getElementById('tags').value = Array.from(selectedTags).join(',');
}

function loadLevels(languageName) {
    const minLevelSelect = document.getElementById('minLevel');
    minLevelSelect.innerHTML = '<option value="" disabled selected>Загрузка...</option>';
    minLevelSelect.disabled = true;

    fetch(`/dashboard/levels?language=${encodeURIComponent(languageName)}`)
        .then(response => {
            if (!response.ok) throw new Error('Ошибка загрузки уровней');
            return response.json();
        })
        .then(levels => {
            minLevelSelect.innerHTML = '<option value="" disabled selected>Выберите уровень</option>';
            levels.forEach(level => {
                const option = document.createElement('option');
                option.value = level;
                option.text = level;
                minLevelSelect.appendChild(option);
            });
            const savedLevel = /*[[${settings['minLevel'] ?: 'null'}]]*/ 'null';
            if (savedLevel !== 'null' && levels.includes(savedLevel)) {
                minLevelSelect.value = savedLevel;
            }
            minLevelSelect.disabled = false;
        })
        .catch(error => {
            console.error('Ошибка загрузки уровней:', error);
            minLevelSelect.innerHTML = '<option value="" disabled selected>Ошибка загрузки</option>';
        });
}

function resetProgram() {
    if (confirm('Вы уверены, что хотите сбросить все настройки программы? Это удалит все активные и выученные слова.')) {
        fetch('/dashboard/reset-settings', { method: 'POST' })
            .then(response => {
                if (response.ok) {
                    location.reload();
                } else {
                    alert('Ошибка при сбросе настроек');
                }
            })
            .catch(error => {
                console.error('Ошибка:', error);
                alert('Ошибка при сбросе настроек');
            });
    }
}

function openProgramModal() {
    const confirmChangeModal = bootstrap.Modal.getInstance(document.getElementById('confirmChangeModal'));
    confirmChangeModal.hide();
    const programModal = new bootstrap.Modal(document.getElementById('programModal'));
    programModal.show();
}

function initProgramModal() {
    const programButton = document.querySelector('button[data-bs-target="#programModal"]');
    const settingsLanguage = /*[[${settings['language'] != null ? 'true' : 'false'}]]*/ false;
    if (programButton) {
        programButton.addEventListener('click', function (event) {
            if (settingsLanguage) {
                event.preventDefault();
                const confirmChangeModal = new bootstrap.Modal(document.getElementById('confirmChangeModal'));
                confirmChangeModal.show();
            }
        });
    }

    const languageSelect = document.getElementById('language');
    if (languageSelect && languageSelect.value) {
        loadLevels(languageSelect.value);
    }

    const tagsInput = document.getElementById('tags');
    if (tagsInput) {
        const initialTags = tagsInput.value ? tagsInput.value.split(',') : [];
        initialTags.forEach(tag => {
            const card = document.querySelector(`.tag-card[data-tag="${tag.trim()}"]`);
            if (card) {
                selectedTags.add(tag.trim());
                card.classList.add('selected');
            }
        });
    }
}