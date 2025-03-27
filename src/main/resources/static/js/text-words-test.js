/**
 * Тестирование API для получения слов из текстов
 */
console.log("Загрузка скрипта для тестирования API слов из текстов...");

// Функция для запуска тестов
function runTests() {
    console.log("Запуск тестов API для слов из текстов...");
    
    // Тест 1: Проверка количества слов из текстов
    testGetTextWordsCount()
        .then(() => testGetTextWords())
        .catch(error => {
            console.error("Ошибка при выполнении тестов:", error);
        });
}

// Тест для получения количества слов из текстов
function testGetTextWordsCount() {
    console.log("Тест 1: Получение количества слов из текстов");
    return fetch('/dashboard/text-words-count')
        .then(response => {
            if (!response.ok) {
                throw new Error(`Ошибка HTTP: ${response.status}`);
            }
            console.log("Статус ответа:", response.status);
            return response.json();
        })
        .then(data => {
            console.log("Результат теста 1:", data);
            console.log(`Количество слов из текстов: ${data.count}`);
            return data;
        })
        .catch(error => {
            console.error("Ошибка в тесте 1:", error);
            throw error;
        });
}

// Тест для получения слов из текстов
function testGetTextWords() {
    console.log("Тест 2: Получение слов из текстов");
    
    // Добавляем метку времени для предотвращения кэширования
    const url = '/dashboard/text-words?_=' + new Date().getTime();
    
    return fetch(url)
        .then(response => {
            console.log("Статус ответа:", response.status);
            if (!response.ok) {
                throw new Error(`Ошибка HTTP: ${response.status}`);
            }
            
            // Проверяем заголовки
            console.log("Заголовки ответа:");
            for (let [key, value] of response.headers.entries()) {
                console.log(`${key}: ${value}`);
            }
            
            return response.json();
        })
        .then(data => {
            console.log("Результат теста 2:", data);
            
            if (data.error) {
                console.error("Ошибка в ответе API:", data.error);
            }
            
            const words = data.words || [];
            const texts = data.texts || [];
            
            console.log(`Количество слов: ${words.length}`);
            console.log(`Количество текстов: ${texts.length}`);
            
            // Вывод информации о первых 5 словах (если есть)
            if (words.length > 0) {
                console.log("Примеры слов:");
                words.slice(0, 5).forEach((word, index) => {
                    console.log(`${index + 1}. ${word.word} - ${word.translation} (из текста: ${word.textTitle || 'Неизвестно'})`);
                });
            }
            
            // Вывод информации о текстах (если есть)
            if (texts.length > 0) {
                console.log("Тексты:");
                texts.forEach((text, index) => {
                    console.log(`${index + 1}. ${text.title} (${text.language}, уровень: ${text.level})`);
                });
            }
            
            // Проверка структуры данных
            console.log("Структура ответа API:", Object.keys(data));
            
            return data;
        })
        .catch(error => {
            console.error("Ошибка в тесте 2:", error);
            throw error;
        });
}

// Запуск тестов при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    console.log("DOM загружен, запускаем тесты...");
    setTimeout(runTests, 1000); // Запускаем тесты через 1 секунду после загрузки страницы
}); 