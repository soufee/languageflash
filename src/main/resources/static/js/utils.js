function shuffleArray(array) {
    for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
    }
    return array;
}

function getActiveWordsCount() {
    if (typeof currentUserId === 'undefined' || currentUserId === null) {
        console.error('Error: currentUserId is not defined.');
        return Promise.resolve(50); // Возвращаем значение по умолчанию
    }
    return fetch(`/api/dashboard/settings?userId=${currentUserId}`, {
        credentials: 'include'
    })
        .then(response => response.json())
        .then(settings => settings.activeWordsCount || 50);
}

function formatTime(seconds) {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes}:${secs < 10 ? '0' : ''}${secs}`;
}

function getRandomWord(words) {
    const totalWeight = words.reduce((sum, word) => sum + word.knowledgeFactor, 0);
    let random = Math.random() * totalWeight;
    for (const word of words) {
        random -= word.knowledgeFactor;
        if (random <= 0) return word;
    }
    return words[0];
}