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

function editText(id) {
    fetch(`/texts/${id}`)
        .then(response => response.json())
        .then(text => {
            document.getElementById('editTextId').value = text.id;
            document.getElementById('editTitleEn').value = text.title;
            document.getElementById('editTitleRu').value = text.translationTitle;
            document.getElementById('editTextEn').value = text.content;
            document.getElementById('editTextRu').value = text.translation;
            document.getElementById('editLanguage').value = text.language;
            document.getElementById('editLevel').value = text.level;
            
            const tagsContainer = document.getElementById('editTags');
            tagsContainer.innerHTML = '';
            text.tags.forEach(tag => {
                const tagElement = document.createElement('span');
                tagElement.className = 'tag-item';
                tagElement.style.backgroundColor = tag.color;
                tagElement.textContent = tag.russianName;
                tagsContainer.appendChild(tagElement);
            });
            
            const modal = new bootstrap.Modal(document.getElementById('editTextModal'));
            modal.show();
        })
        .catch(error => {});
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