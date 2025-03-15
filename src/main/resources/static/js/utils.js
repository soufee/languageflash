function shuffleArray(array) {
    for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
    }
    return array;
}

function getActiveWordsCount() {
    return fetch('/dashboard/settings')
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