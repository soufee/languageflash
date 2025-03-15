function updateDashboardCounts() {
    fetch('/dashboard/active-words-json')
        .then(response => response.json())
        .then(activeWords => {
            const activeCount = activeWords.length;
            document.getElementById('activeWordsCount').textContent = activeCount;
            console.log('Updated active words count:', activeCount);
            fetch('/learn/refill', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                }
            })
                .then(response => response.json())
                .then(data => {
                    document.getElementById('learnedWordsCount').textContent = data.learnedCount;
                    console.log('Updated learned words count:', data.learnedCount);
                });
        })
        .catch(error => console.error('Error updating dashboard counts:', error));
}

document.addEventListener('DOMContentLoaded', function () {
    initProgramModal();
    initLearnModal();
    initFlashLearnModal();
    initActiveWordsModal();
    initLearnedWordsModal();

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