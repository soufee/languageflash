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

function submitProgramForm(event) {
    // First, log everything about the event and environment
    console.log('=== submitProgramForm START (from programModal.js) ===');
    console.log('Event:', event);
    console.log('Event type:', event.type);
    console.log('Event target:', event.target);
    
    // Always prevent default first
    event.preventDefault();
    console.log('Form submission prevented');
    
    // Log current user ID availability 
    console.log('typeof currentUserId:', typeof currentUserId);
    console.log('currentUserId value:', currentUserId);
    console.log('window.currentUserId:', window.currentUserId);
    
    // Try to find currentUserId in different ways
    let userId = null;
    if (typeof currentUserId !== 'undefined' && currentUserId !== null) {
        userId = currentUserId;
        console.log('Using currentUserId:', userId);
    } else if (typeof window.currentUserId !== 'undefined' && window.currentUserId !== null) {
        userId = window.currentUserId;
        console.log('Using window.currentUserId:', userId);
    } else {
        console.error('=== CRITICAL ERROR: currentUserId is not defined ===');
        console.log('Available global variables:', Object.keys(window));
        alert('Ошибка: ID пользователя не найден. Попробуйте перезагрузить страницу.');
        return false;
    }
    
    const form = event.target;
    console.log('Form element:', form);
    
    const formData = new FormData(form);
    console.log('FormData created');
    
    // Log all form data entries
    for (let [key, value] of formData.entries()) {
        console.log(`FormData ${key}:`, value);
    }
    
    const data = {
        language: formData.get('language'),
        minLevel: formData.get('minLevel'),
        activeWordsCount: parseInt(formData.get('activeWordsCount')),
        tags: formData.get('tags') ? formData.get('tags').split(',').map(tag => tag.trim()).filter(tag => tag.length > 0) : []
    };
    
    console.log('Processed form data:', data);
    
    // Validate required fields
    if (!data.language || !data.minLevel) {
        console.error('Validation failed - missing required fields');
        alert('Пожалуйста, выберите язык и минимальный уровень');
        return false;
    }
    
    // Show loading state
    const submitButton = form.querySelector('button[type="submit"]');
    const originalText = submitButton.textContent;
    submitButton.disabled = true;
    submitButton.textContent = 'Сохранение...';
    console.log('Button state changed to loading');
    
    const url = '/api/dashboard/settings?userId=' + userId;
    console.log('=== MAKING FETCH REQUEST ===');
    console.log('URL:', url);
    console.log('Method: POST');
    console.log('Body:', JSON.stringify(data));
    
    return fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(data)
    })
    .then(response => {
        console.log('=== FETCH RESPONSE RECEIVED ===');
        console.log('Response status:', response.status);
        console.log('Response ok:', response.ok);
        console.log('Response headers:', response.headers);
        
        if (response.ok) {
            console.log('Settings saved successfully');
            
            // Close modal
            const modal = bootstrap.Modal.getInstance(document.getElementById('programModal'));
            if (modal) {
                console.log('Hiding modal');
                modal.hide();
            } else {
                console.warn('Modal instance not found');
            }
            
            // Update dashboard counts
            if (typeof updateDashboardCounts === 'function') {
                console.log('Updating dashboard counts');
                updateDashboardCounts();
            } else {
                console.warn('updateDashboardCounts function not found');
            }
            
            // Show success message
            alert('Настройки программы успешно сохранены! Слова для изучения добавлены.');
            
            // Reload page to show updated program info
            console.log('Reloading page');
            location.reload();
            
            return true;
        } else {
            console.error('=== FETCH ERROR RESPONSE ===');
            console.error('Error status:', response.status);
            console.error('Error status text:', response.statusText);
            
            return response.text().then(text => {
                console.error('Error response body:', text);
                alert('Ошибка при сохранении настроек. Код ошибки: ' + response.status + '. ' + text);
                return false;
            });
        }
    })
    .catch(error => {
        console.error('=== FETCH NETWORK ERROR ===');
        console.error('Network error:', error);
        console.error('Error message:', error.message);
        console.error('Error stack:', error.stack);
        alert('Ошибка сети при сохранении настроек: ' + error.message);
        return false;
    })
    .finally(() => {
        console.log('=== FETCH FINALLY BLOCK ===');
        // Restore button state
        submitButton.disabled = false;
        submitButton.textContent = originalText;
        console.log('Button state restored');
        console.log('=== submitProgramForm END ===');
    });
}

function loadLevels(languageName) {
    const minLevelSelect = document.getElementById('minLevel');
    minLevelSelect.innerHTML = '<option value="" disabled selected>Загрузка...</option>';
    minLevelSelect.disabled = true;

    fetch(`/api/dashboard/languages/levels?language=${encodeURIComponent(languageName)}`)
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
            
            // Проверяем сохраненный уровень из настроек
            const savedLevelElement = document.querySelector('input[name="savedMinLevel"]');
            const savedLevel = savedLevelElement ? savedLevelElement.value : null;
            if (savedLevel && levels.includes(savedLevel)) {
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
        if (typeof currentUserId === 'undefined' || currentUserId === null) {
            console.error('Error: currentUserId is not defined.');
            alert('Ошибка: ID пользователя не найден. Невозможно сбросить настройки.');
            return;
        }
        fetch(`/api/dashboard/settings/reset?userId=${currentUserId}`, { method: 'POST' })
            .then(response => {
                if (response.ok) {
                    location.reload();
                } else {
                    alert('Ошибка при сбросе настроек');
                    console.error('Ошибка при сбросе настроек:', response.status, response.statusText);
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
    const settingsLanguage = /*[[${settings != null and settings.containsKey('language') ? 'true' : 'false'}]]*/ false;
    if (programButton) {
        programButton.addEventListener('click', function (event) {
            if (settingsLanguage) {
                event.preventDefault();
                const confirmChangeModal = new bootstrap.Modal(document.getElementById('confirmChangeModal'));
                confirmChangeModal.show();
            }
        });
    }

    // Добавляем обработчик события показа модального окна
    const programModal = document.getElementById('programModal');
    if (programModal) {
        programModal.addEventListener('shown.bs.modal', function () {
            const languageSelect = document.getElementById('language');
            if (languageSelect && languageSelect.value) {
                loadLevels(languageSelect.value);
            }
            
            // Инициализируем выбранные теги
            const tagsInput = document.getElementById('tags');
            if (tagsInput) {
                selectedTags.clear(); // Очищаем предыдущие выборы
                const initialTags = tagsInput.value ? tagsInput.value.split(',') : [];
                initialTags.forEach(tag => {
                    const trimmedTag = tag.trim();
                    if (trimmedTag) {
                        const card = document.querySelector(`.tag-card[data-tag="${trimmedTag}"]`);
                        if (card) {
                            selectedTags.add(trimmedTag);
                            card.classList.add('selected');
                        }
                    }
                });
            }
        });
    }
}