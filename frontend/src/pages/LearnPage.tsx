import { useCallback, useEffect, useState } from 'react';
import api from '../api/client';
import { t } from '../i18n';

interface Entry {
  id: number;
  word: string;
  translation: string;
  example?: string;
  exampleTranslation?: string;
  knowledgeFactor: number;
}

export default function LearnPage() {
  const [current, setCurrent] = useState<Entry | null>(null);
  const [revealed, setRevealed] = useState(false);
  const [flying, setFlying] = useState(false);
  const [activeCount, setActiveCount] = useState(0);
  const [sessionAnswers, setSessionAnswers] = useState(0);
  const [done, setDone] = useState(false);

  const loadNext = useCallback(async () => {
    const [next, active] = await Promise.all([api.get('/learn/next'), api.get('/learn/active')]);
    setActiveCount(active.data.length);
    if (next.data.word) {
      setCurrent(next.data.word);
      setRevealed(false);
      setDone(false);
    } else {
      setCurrent(null);
      setDone(true);
    }
  }, []);

  useEffect(() => {
    loadNext();
  }, [loadNext]);

  async function answer(knows: boolean) {
    if (!current) return;
    if (knows) {
      setFlying(true);
      await new Promise((r) => setTimeout(r, 400));
      setFlying(false);
    }
    await api.post('/learn/answer', { entryId: current.id, knows });
    setSessionAnswers((n) => n + 1);
    await loadNext();
  }

  async function refill() {
    await api.post('/learn/refill');
    await loadNext();
  }

  if (done || !current) {
    return (
      <div className="flashcard card">
        <h2>{done && sessionAnswers > 0 ? '🎉 Порция пройдена!' : 'Нет слов для тренировки'}</h2>
        <p className="dim">
          {activeCount === 0
            ? 'Активная порция пуста. Добавьте слова в словарь и нажмите «Пополнить порцию».'
            : 'Все слова на сегодня повторены.'}
        </p>
        <button className="btn primary lg" onClick={refill}>
          Пополнить порцию
        </button>
      </div>
    );
  }

  return (
    <div>
      <div className="row spread">
        <span className="dim small">В порции: {activeCount} слов · ответов за сессию: {sessionAnswers}</span>
      </div>
      <div className={`flashcard card ${flying ? 'fly-away' : ''}`}>
        <div className="word">{current.word}</div>
        {revealed ? (
          <>
            <div className="translation">{current.translation}</div>
            {current.example && (
              <div className="example">
                {current.example}
                {current.exampleTranslation && <div>{current.exampleTranslation}</div>}
              </div>
            )}
            <div className="row mt" style={{ justifyContent: 'center' }}>
              <button className="btn success lg" onClick={() => answer(true)}>
                ✓ {t('know')}
              </button>
              <button className="btn danger lg" onClick={() => answer(false)}>
                ✗ {t('dontKnow')}
              </button>
            </div>
          </>
        ) : (
          <div className="mt">
            <button className="btn primary lg" onClick={() => setRevealed(true)}>
              {t('showAnswer')}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
