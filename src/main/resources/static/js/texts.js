function viewText(id) {
    console.log('We are opening text with id: ' + id);
    fetch(`/texts/${id}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(text => {
            if (!text) {
                throw new Error('Text not found');
            }

            console.log('Received text data:', text);
            console.log('Tags data:', text.tags);
            console.log('TagsAsSet data:', text.tagsAsSet);
            console.log('DTOTagsAsSet data:', text.dtoTagsAsSet);

            const modalElement = document.getElementById('viewTextModal');
            if (!modalElement) {
                console.error('Modal element not found');
                return;
            }

            // Проверяем существование всех необходимых элементов
            const elements = {
                'viewTitle': text.title,
                'viewTextEn': text.content,
                'viewTextRu': text.translation,
                'viewLanguage': text.language ? text.language.name : '',
                'viewLevel': text.level ? `(${text.level})` : ''
            };

            for (const [elementId, value] of Object.entries(elements)) {
                const element = document.getElementById(elementId);
                if (element) {
                    if (elementId.includes('Text')) {
                        element.innerHTML = value;
                    } else {
                        element.textContent = value;
                    }
                } else {
                    console.error(`Element with id ${elementId} not found`);
                }
            }
            
            // Обработка тегов
            const tagsContainer = document.getElementById('viewTags');
            if (tagsContainer) {
                tagsContainer.innerHTML = '';
                
                // Обрабатываем разные варианты получения тегов
                let tagsList = [];
                
                // Вариант 1: Используем готовый массив dtoTagsAsSet
                if (text.dtoTagsAsSet && Array.isArray(text.dtoTagsAsSet)) {
                    console.log('Using dtoTagsAsSet array directly');
                    tagsList = text.dtoTagsAsSet;
                }
                // Вариант 2: Используем tagsAsSet, но нужно преобразовать объекты
                else if (text.tagsAsSet && Array.isArray(text.tagsAsSet)) {
                    console.log('Using tagsAsSet array with transformation');
                    tagsList = text.tagsAsSet.map(tag => {
                        // Если это объект с полями name и russianName
                        if (tag && typeof tag === 'object') {
                            return {
                                name: tag.name || '',
                                russianName: tag.russianName || getTagRussianName(tag.name) || '',
                                color: tag.color || getTagColor(tag.name) || '#6c757d'
                            };
                        }
                        // Если это строка (название тега)
                        return {
                            name: tag,
                            russianName: getTagRussianName(tag),
                            color: getTagColor(tag)
                        };
                    });
                }
                // Вариант 3: Есть строка tags, которую нужно распарсить
                else if (text.tags) {
                    console.log('Trying to parse tags string:', text.tags);
                    try {
                        // Если tags это JSON строка
                        if (typeof text.tags === 'string' && (text.tags.startsWith('[') || text.tags.startsWith('{'))) {
                            tagsList = JSON.parse(text.tags);
                            console.log('Parsed tags JSON:', tagsList);
                            
                            // Преобразуем в нужный формат
                            tagsList = tagsList.map(tag => {
                                if (typeof tag === 'string') {
                                    return {
                                        name: tag,
                                        russianName: getTagRussianName(tag),
                                        color: getTagColor(tag)
                                    };
                                }
                                return tag;
                            });
                        } 
                        // Если tags это строка с перечисленными тегами через запятую
                        else if (typeof text.tags === 'string') {
                            const tagNames = text.tags.split(',').map(tag => tag.trim());
                            console.log('Split tag names:', tagNames);
                            
                            // Преобразуем имена тегов в объекты с цветом и русским названием
                            tagsList = tagNames.map(tagName => {
                                return {
                                    name: tagName,
                                    russianName: getTagRussianName(tagName),
                                    color: getTagColor(tagName)
                                };
                            });
                            console.log('Created tag objects:', tagsList);
                        }
                    } catch (error) {
                        console.error('Error parsing tags:', error);
                    }
                }
                
                if (tagsList.length > 0) {
                    tagsList.forEach(tag => {
                        const tagElement = document.createElement('span');
                        tagElement.className = 'tag-item';
                        
                        // Используем данные из объекта тега
                        tagElement.style.backgroundColor = tag.color || getTagColor(tag.name) || '#6c757d';
                        tagElement.textContent = tag.russianName || getTagRussianName(tag.name) || tag.name;
                        
                        tagsContainer.appendChild(tagElement);
                    });
                } else {
                    tagsContainer.innerHTML = '<span class="text-muted">Нет тем</span>';
                }
            }
            
            // Обработка слов
            const wordsContainer = document.getElementById('viewWords');
            if (wordsContainer) {
                wordsContainer.innerHTML = '';
                if (text.words && Array.isArray(text.words)) {
                    text.words.forEach(word => {
                        const wordElement = document.createElement('div');
                        wordElement.className = 'word-item';
                        wordElement.innerHTML = `<strong>${word.word}</strong> - ${word.translation}`;
                        wordsContainer.appendChild(wordElement);
                    });
                }
                
                if (!text.words || text.words.length === 0) {
                    wordsContainer.innerHTML = '<span class="text-muted">Нет слов</span>';
                }
            }
            
            // Показываем модальное окно
            const modal = new bootstrap.Modal(modalElement);
            modal.show();
            
            // Добавляем обработчики для подсветки
            const matchElements = modalElement.querySelectorAll('[data-match-id]');
            matchElements.forEach(element => {
                element.addEventListener('mouseenter', function() {
                    const matchId = this.getAttribute('data-match-id');
                    const matches = modalElement.querySelectorAll(`[data-match-id="${matchId}"]`);
                    matches.forEach(match => match.classList.add('highlighted'));
                });
                
                element.addEventListener('mouseleave', function() {
                    const matchId = this.getAttribute('data-match-id');
                    const matches = modalElement.querySelectorAll(`[data-match-id="${matchId}"]`);
                    matches.forEach(match => match.classList.remove('highlighted'));
                });
            });
        })
        .catch(error => {
            console.error('Error fetching text:', error);
            alert('Произошла ошибка при загрузке текста. Пожалуйста, попробуйте позже.');
        });
}

// Массив для хранения слов текста при редактировании
let editTextWords = [];
// Массив для хранения ID удаленных слов
let deletedWordIds = [];

function editText(id) {
    console.log('We are editing text with id: ' + id);
    // Сбрасываем массивы при новом редактировании
    editTextWords = [];
    deletedWordIds = [];
    
    fetch(`/texts/${id}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(text => {
            console.log('Received text data for edit:', text);
            
            // Заполняем поля в модальном окне
            document.getElementById('editTextId').value = text.id;
            document.getElementById('editTitle').value = text.title || '';
            document.getElementById('editContent').value = text.content || '';
            document.getElementById('editTranslation').value = text.translation || '';
            
            // Проверяем наличие элементов для языка и уровня
            const editLanguageElement = document.getElementById('editLanguage');
            if (editLanguageElement) {
                const languageValue = text.language ? (text.language.name || '') : '';
                editLanguageElement.value = languageValue;
            }
            
            const editLevelElement = document.getElementById('editLevel');
            if (editLevelElement) {
                editLevelElement.value = text.level || '';
            }
            
            // Получаем список тегов для выбора в селекте
            let tagNames = [];
            
            // Проверяем разные варианты получения тегов
            if (text.dtoTagsAsSet && Array.isArray(text.dtoTagsAsSet)) {
                tagNames = text.dtoTagsAsSet.map(tag => tag.name);
            } else if (text.tagsAsSet && Array.isArray(text.tagsAsSet)) {
                tagNames = text.tagsAsSet.map(tag => typeof tag === 'string' ? tag : tag.name);
            } else if (text.tags) {
                // Если tags это строка с тегами
                if (typeof text.tags === 'string') {
                    // Пытаемся сначала распарсить JSON
                    try {
                        if (text.tags.startsWith('[') || text.tags.startsWith('{')) {
                            const parsedTags = JSON.parse(text.tags);
                            tagNames = Array.isArray(parsedTags) ? 
                                parsedTags.map(tag => typeof tag === 'string' ? tag : tag.name) :
                                [];
                        } else {
                            // Если не JSON, то просто разделяем запятой
                            tagNames = text.tags.split(',').map(t => t.trim());
                        }
                    } catch (error) {
                        console.error('Error parsing tags:', error);
                        // Если ошибка парсинга, просто разделяем запятой
                        tagNames = text.tags.split(',').map(t => t.trim());
                    }
                } else if (Array.isArray(text.tags)) {
                    // Если tags уже массив
                    tagNames = text.tags.map(tag => typeof tag === 'string' ? tag : tag.name);
                }
            }
            
            // Выбираем соответствующие теги в селекте
            const tagsSelect = document.getElementById('editTagsSelect');
            if (tagsSelect) {
                // Сначала снимаем все выделения
                Array.from(tagsSelect.options).forEach(option => {
                    option.selected = false;
                });
                
                // Затем выбираем нужные теги
                tagNames.forEach(tagName => {
                    Array.from(tagsSelect.options).forEach(option => {
                        if (option.value === tagName) {
                            option.selected = true;
                        }
                    });
                });
            }
            
            // Загружаем слова текста
            if (text.words && Array.isArray(text.words)) {
                editTextWords = [...text.words];
                renderDictionaryTable();
            } else {
                console.warn('No words found in text:', text.id);
                editTextWords = [];
                renderDictionaryTable();
            }
            
            // Получаем экземпляр модального окна с DOM и показываем его
            const modalElement = document.getElementById('editTextModal');
            if (modalElement) {
                const modal = new bootstrap.Modal(modalElement);
                modal.show();
            } else {
                console.error('Edit modal element not found in the DOM');
            }
        })
        .catch(error => {
            console.error('Error fetching text for edit:', error);
            alert('Произошла ошибка при загрузке текста для редактирования.');
        });
}

// Функция для отрисовки таблицы со словами
function renderDictionaryTable() {
    const tableBody = document.getElementById('editDictionaryBody');
    if (!tableBody) {
        console.error('Dictionary table body not found');
        return;
    }
    
    // Очищаем таблицу
    tableBody.innerHTML = '';
    
    // Если нет слов, выводим сообщение
    if (editTextWords.length === 0) {
        const emptyRow = document.createElement('tr');
        emptyRow.innerHTML = '<td colspan="5" class="text-center">Словарь текста пуст. Добавьте слова нажав на кнопку "Добавить слово".</td>';
        tableBody.appendChild(emptyRow);
        return;
    }
    
    // Добавляем строки для каждого слова
    editTextWords.forEach((word, index) => {
        const row = document.createElement('tr');
        row.setAttribute('data-word-index', index);
        
        // Добавляем word-id в data-атрибут, если он есть
        if (word.id) {
            row.setAttribute('data-word-id', word.id);
        }
        
        row.innerHTML = `
            <td><input type="text" class="form-control word-text" value="${word.word || ''}" onchange="updateWordField(${index}, 'word', this.value)"></td>
            <td><input type="text" class="form-control word-translation" value="${word.translation || ''}" onchange="updateWordField(${index}, 'translation', this.value)"></td>
            <td><input type="text" class="form-control word-example" value="${word.exampleSentence || ''}" onchange="updateWordField(${index}, 'exampleSentence', this.value)"></td>
            <td><input type="text" class="form-control word-example-translation" value="${word.exampleTranslation || ''}" onchange="updateWordField(${index}, 'exampleTranslation', this.value)"></td>
            <td>
                <button type="button" class="btn btn-outline-danger btn-sm word-action-btn" onclick="removeWordRow(${index})">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        `;
        
        tableBody.appendChild(row);
    });
}

// Функция для обновления поля слова
function updateWordField(index, field, value) {
    if (index >= 0 && index < editTextWords.length) {
        editTextWords[index][field] = value;
    }
}

// Функция для добавления новой строки слова
function addNewWordRow() {
    const newWord = {
        word: '',
        translation: '',
        exampleSentence: '',
        exampleTranslation: ''
    };
    
    editTextWords.push(newWord);
    renderDictionaryTable();
    
    // Фокусируемся на первом поле последней добавленной строки
    const tableBody = document.getElementById('editDictionaryBody');
    if (tableBody && tableBody.lastElementChild) {
        const lastRow = tableBody.lastElementChild;
        const firstInput = lastRow.querySelector('input');
        if (firstInput) {
            firstInput.focus();
        }
    }
}

// Функция для удаления строки слова
function removeWordRow(index) {
    console.log('Removing word at index:', index);
    if (index >= 0 && index < editTextWords.length) {
        // Если у слова есть ID, сохраняем его для последующего удаления
        if (editTextWords[index].id) {
            // Преобразуем ID в число
            const wordId = parseInt(editTextWords[index].id, 10);
            if (!isNaN(wordId)) {
                console.log('Adding word ID to deletedWordIds:', wordId);
                deletedWordIds.push(wordId);
                console.log('Current deletedWordIds:', deletedWordIds);
            } else {
                console.warn('Invalid word ID:', editTextWords[index].id);
            }
        } else {
            console.log('Word has no ID, nothing to add to deletedWordIds');
        }
        
        // Удаляем слово из массива
        const removedWord = editTextWords[index];
        console.log('Removing word from editTextWords:', removedWord);
        editTextWords.splice(index, 1);
        console.log('Words remaining in editTextWords:', editTextWords.length);
        renderDictionaryTable();
    } else {
        console.error('Invalid index for removeWordRow:', index);
    }
}

function submitEditTextForm() {
    console.log('Submitting edit text form...');
    
    // Проверка, что все поля заполнены
    const editTitle = document.getElementById('editTitle').value.trim();
    const editContent = document.getElementById('editContent').value.trim();
    const editTranslation = document.getElementById('editTranslation').value.trim();
    const editLanguage = document.getElementById('editLanguage').value;
    const editLevel = document.getElementById('editLevel').value;
    const editTagsSelect = document.getElementById('editTagsSelect');
    const editTextId = document.getElementById('editTextId').value;

    console.log('Edit form values:', {
        editTextId,
        editTitle,
        editLanguage,
        editLevel
    });

    if (!editTitle || !editContent || !editTranslation || !editLanguage || !editLevel) {
        showErrorMessage('Пожалуйста, заполните все поля формы');
        return;
    }

    // Собираем выбранные теги - просто как строку с разделителями-запятыми
    const selectedTagsArray = [];
    for (let i = 0; i < editTagsSelect.options.length; i++) {
        if (editTagsSelect.options[i].selected) {
            selectedTagsArray.push(editTagsSelect.options[i].value);
        }
    }
    // Объединяем теги в строку через запятую для сервера
    const selectedTags = selectedTagsArray.join(',');
    console.log('Selected tags:', selectedTagsArray);
    console.log('Tags string:', selectedTags);

    // Собираем данные о словах из массива editTextWords
    const wordsData = [];
    
    // Проверяем каждое слово на валидность
    for (let i = 0; i < editTextWords.length; i++) {
        const word = editTextWords[i];
        
        // Проверяем, что у слова есть обязательные поля
        if (word && word.word && word.word.trim() && word.translation && word.translation.trim()) {
            const wordData = {
                id: word.id || '',
                word: word.word.trim(),
                translation: word.translation.trim(),
                exampleSentence: word.exampleSentence || '',
                exampleTranslation: word.exampleTranslation || ''
            };
            
            wordsData.push(wordData);
        } else {
            console.warn(`Skipping invalid word at index ${i}:`, word);
        }
    }

    console.log('Words data:', wordsData);
    console.log('Deleted word IDs:', deletedWordIds);

    const formData = {
        id: editTextId,
        title: editTitle,
        content: editContent,
        translation: editTranslation,
        language: editLanguage,
        level: editLevel,
        tags: selectedTags, // Отправляем теги как строку через запятую
        words: wordsData,
        deletedWordIds: deletedWordIds
    };

    console.log('Sending form data:', formData);

    fetch('/admin/texts/edit', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(formData)
    })
    .then(response => {
        console.log('Response status:', response.status);
        return response.json();
    })
    .then(data => {
        console.log('Response data:', data);
        if (data.status === 'success') {
            // Успешное обновление
            const modalElement = document.getElementById('editTextModal');
            if (modalElement) {
                const modalInstance = bootstrap.Modal.getInstance(modalElement);
                if (modalInstance) {
                    modalInstance.hide();
                }
            }
            loadTexts(); // Перезагружаем список текстов
            // Сбрасываем массивы после успешного сохранения
            editTextWords = [];
            deletedWordIds = [];
        } else {
            // Ошибка
            showErrorMessage('Ошибка при обновлении текста: ' + (data.message || 'Неизвестная ошибка'));
        }
    })
    .catch(error => {
        console.error('Error submitting form:', error);
        showErrorMessage('Ошибка при отправке данных: ' + error);
    });
}

function deleteText(id) {
    if (confirm('Вы уверены, что хотите удалить этот текст?')) {
        fetch(`/texts/${id}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (response.ok) {
                    window.location.reload();
                }
            })
            .catch(error => {});
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        modal.addEventListener('hidden.bs.modal', function () {
            document.activeElement.blur();
            setTimeout(() => {
                document.body.focus();
            }, 10);
        });
    });
});

// Вспомогательные функции для работы с тегами
function getTagRussianName(tagName) {
    const tagNames = {
        'BUSINESS': 'Деловое общение',
        'SCIENCE': 'Наука',
        'LITERATURE': 'Литература',
        'TRAVEL': 'Путешествия',
        'MUSIC': 'Музыка',
        'TECHNOLOGY': 'Технологии',
        'SPORT': 'Спорт',
        'FOOD': 'Еда',
        'HISTORY': 'История',
        'CULTURE': 'Культура',
        'JOURNALISM': 'Публицистика',
        'TECHNICAL_SCIENCES': 'Технические науки',
        'INTERNET': 'Интернет',
        'ART': 'Искусство',
        'IRREGULAR_VERBS': 'Неправильные глаголы',
        'REGULAR_VERBS': 'Правильные глаголы',
        'FAMILY_HOME': 'Семья, дом и быт',
        'SONG_LYRICS': 'Тексты песен',
        'PHILOSOPHY': 'Философия',
        'MEDICINE': 'Медицина',
        'BASIC_VOCABULARY': 'Базовая лексика',
        'MOVIES': 'Фильмы',
        'SPORTS': 'Спорт'
    };
    return tagNames[tagName] || tagName;
}

function getTagColor(tagName) {
    const tagColors = {
        'BUSINESS': '#3F51B5',
        'SCIENCE': '#673AB7',
        'LITERATURE': '#2196F3',
        'TRAVEL': '#FF9800',
        'MUSIC': '#6f42c1',
        'TECHNOLOGY': '#20c997',
        'SPORT': '#0dcaf0',
        'FOOD': '#d63384',
        'HISTORY': '#607D8B',
        'CULTURE': '#ffc107',
        'JOURNALISM': '#CDDC39',
        'TECHNICAL_SCIENCES': '#9C27B0',
        'INTERNET': '#FF5722',
        'ART': '#fd7e14',
        'IRREGULAR_VERBS': '#FF6F61',
        'REGULAR_VERBS': '#4CAF50',
        'FAMILY_HOME': '#FFB300',
        'SONG_LYRICS': '#E91E63',
        'PHILOSOPHY': '#795548',
        'MEDICINE': '#00BCD4',
        'BASIC_VOCABULARY': '#8BC34A',
        'MOVIES': '#F44336',
        'SPORTS': '#009688'
    };
    return tagColors[tagName] || '#6c757d';
}

// Утилитарные функции для показа сообщений об ошибках и успешных операциях
function showErrorMessage(message) {
    alert('Ошибка: ' + message);
    console.error(message);
}


/**
 * Загружает список текстов с сервера и обновляет интерфейс
 */
function loadTexts() {
    window.location.href = '/texts';
}

function addText() {
    const formData = new FormData(document.getElementById('addTextForm'));
    const requestBody = {
        title: formData.get('title'),
        language: formData.get('language'),
        level: formData.get('level'),
        tags: formData.get('tags'),
        content: formData.get('content'),
        translation: formData.get('translation'),
        words: []
    };

    fetch('/api/texts/add', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            loadTexts();
        } else {
            alert(data.message || 'Произошла ошибка при добавлении текста');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Произошла ошибка при добавлении текста');
    });
}

function editText(textId) {
    fetch(`/api/texts/${textId}`)
        .then(response => response.json())
        .then(text => {
            document.getElementById('editTextId').value = text.id;
            document.getElementById('editTextTitle').value = text.title;
            document.getElementById('editTextLanguage').value = text.language.name;
            document.getElementById('editTextLevel').value = text.level;
            document.getElementById('editTextTags').value = text.tags;
            document.getElementById('editTextContent').value = text.content;
            document.getElementById('editTextTranslation').value = text.translation;
            
            editTextWords = text.words;
            deletedWordIds = [];
            
            renderDictionaryTable();
            
            $('#editTextModal').modal('show');
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Произошла ошибка при загрузке текста');
        });
}

function submitEditTextForm() {
    const formData = new FormData(document.getElementById('editTextForm'));
    const requestBody = {
        id: formData.get('id'),
        title: formData.get('title'),
        language: formData.get('language'),
        level: formData.get('level'),
        tags: formData.get('tags'),
        content: formData.get('content'),
        translation: formData.get('translation'),
        words: editTextWords,
        deletedWordIds: deletedWordIds
    };

    fetch('/api/texts/edit', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(requestBody)
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            $('#editTextModal').modal('hide');
            loadTexts();
        } else {
            alert(data.message || 'Произошла ошибка при редактировании текста');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Произошла ошибка при редактировании текста');
    });
}

function deleteText(textId) {
    if (confirm('Вы уверены, что хотите удалить этот текст?')) {
        fetch('/api/texts/delete', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ textId: textId })
        })
        .then(response => response.json())
        .then(data => {
            if (data.status === 'success') {
                loadTexts();
            } else {
                alert(data.message || 'Произошла ошибка при удалении текста');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('Произошла ошибка при удалении текста');
        });
    }
}

function takeTextToWork(textId) {
    fetch('/api/texts/take-to-work', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ textId: textId })
    })
    .then(response => response.json())
    .then(data => {
        if (data.status === 'success') {
            window.location.href = '/learn';
        } else {
            alert(data.message || 'Произошла ошибка при взятии текста в работу');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Произошла ошибка при взятии текста в работу');
    });
}