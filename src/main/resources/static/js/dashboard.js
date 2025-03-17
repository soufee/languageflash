function updateDashboardCounts() {
    fetch('/dashboard/active-words-json')
        .then(response => response.text())
        .then(data => {
            const activeWords = JSON.parse(data);
            const activeCount = activeWords.length;
            document.getElementById('activeWordsCount').textContent = activeCount;
            console.log('Updated active words count:', activeCount);

            fetch('/dashboard/learned-words-json', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            })
                .then(response => response.text())
                .then(data => {
                    const learnedWords = JSON.parse(data);
                    const learnedCount = learnedWords.length;
                    document.getElementById('learnedWordsCount').textContent = learnedCount;
                    console.log('Updated learned words count:', learnedCount);
                })
                .catch(error => console.error('Ошибка загрузки выученных слов:', error));
        })
        .catch(error => console.error('Ошибка загрузки активных слов:', error));
}

document.addEventListener('DOMContentLoaded', function () {
    initProgramModal();
    initFlashLearnModal();
    const showTagPrompt = /*[[${showTagPrompt != null ? showTagPrompt : false}]]*/ false;
    if (showTagPrompt) {
        const addTagsModal = new bootstrap.Modal(document.getElementById('addTagsModal'));
        const modalBody = document.getElementById('addTagsModal').querySelector('.modal-body');
        const activeWordsCount = /*[[${progressCount}]]*/ 0;
        const targetWordsCount = /*[[${settings['activeWordsCount'] ?: 50}]]*/ 50;
        const prompt = document.createElement('p');
        prompt.className = 'prompt-message';
        prompt.innerHTML = `Для вашей программы найдено только ${activeWordsCount} слов. Чтобы начать обучение, нужно как минимум ${targetWordsCount} слов. Пожалуйста, выберите дополнительные темы:`;
        modalBody.insertBefore(prompt, modalBody.querySelector('form'));
        addTagsModal.show();
    }
});