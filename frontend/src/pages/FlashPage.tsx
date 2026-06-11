import { useEffect, useRef, useState } from 'react';
import api from '../api/client';

interface Entry {
  word: string;
  translation: string;
}

/** Режим 25-го кадра (RSVP, ТЗ 3.3): быстрый показ пар слово-перевод. */
export default function FlashPage() {
  const [settings, setSettings] = useState({ flashSpeedMs: 1000, flashDurationMin: 3, flashMode: 'MIXED' });
  const [words, setWords] = useState<Entry[]>([]);
  const [running, setRunning] = useState(false);
  const [index, setIndex] = useState(0);
  const [direct, setDirect] = useState(true);
  const [progress, setProgress] = useState(0);
  const timerRef = useRef<ReturnType<typeof setInterval>>();
  const endRef = useRef(0);

  useEffect(() => {
    api.get('/user/me/settings').then((res) => {
      setSettings({
        flashSpeedMs: res.data.flashSpeedMs ?? 1000,
        flashDurationMin: res.data.flashDurationMin ?? 3,
        flashMode: res.data.flashMode ?? 'MIXED',
      });
    });
    return () => clearInterval(timerRef.current);
  }, []);

  async function start() {
    const res = await api.get('/learn/flash-words', { params: { source: 'ACTIVE', limit: 300 } });
    if (res.data.length === 0) {
      alert('Активная порция пуста — добавьте слова и пополните порцию на странице «Тренировка»');
      return;
    }
    // сохраняем настройки между сеансами (ТЗ 3.3.7)
    await api.put('/user/me/settings', settings);
    setWords(res.data);
    setIndex(0);
    setRunning(true);
    endRef.current = Date.now() + settings.flashDurationMin * 60_000;

    timerRef.current = setInterval(() => {
      const left = endRef.current - Date.now();
      if (left <= 0) {
        stop();
        return;
      }
      setProgress(1 - left / (settings.flashDurationMin * 60_000));
      setIndex((i) => i + 1);
      if (settings.flashMode === 'MIXED') setDirect(Math.random() > 0.5);
      else setDirect(settings.flashMode === 'DIRECT');
    }, settings.flashSpeedMs);
  }

  function stop() {
    clearInterval(timerRef.current);
    setRunning(false);
  }

  if (running && words.length > 0) {
    const entry = words[index % words.length];
    return (
      <div className="rsvp-screen" onClick={stop}>
        <div key={index}>
          <div className="rsvp-word">{direct ? entry.word : entry.translation}</div>
          <div className="rsvp-sub">{direct ? entry.translation : entry.word}</div>
        </div>
        <div className="dim small" style={{ position: 'fixed', top: 20, right: 24 }}>
          клик — остановить
        </div>
        <div className="rsvp-progress">
          <div style={{ width: `${progress * 100}%` }} />
        </div>
      </div>
    );
  }

  return (
    <div className="form card" style={{ marginTop: 24 }}>
      <h2>⚡ Режим 25-го кадра</h2>
      <p className="dim small">
        Быстрое последовательное предъявление пар «слово — перевод» из вашей активной порции.
      </p>
      <label>Скорость показа: {settings.flashSpeedMs} мс</label>
      <input
        type="range"
        min={100}
        max={3000}
        step={100}
        value={settings.flashSpeedMs}
        onChange={(e) => setSettings({ ...settings, flashSpeedMs: +e.target.value })}
      />
      <label>Длительность сеанса</label>
      <select
        value={settings.flashDurationMin}
        onChange={(e) => setSettings({ ...settings, flashDurationMin: +e.target.value })}
      >
        <option value={1}>1 минута</option>
        <option value={3}>3 минуты</option>
        <option value={5}>5 минут</option>
        <option value={10}>10 минут</option>
      </select>
      <label>Режим показа</label>
      <select value={settings.flashMode} onChange={(e) => setSettings({ ...settings, flashMode: e.target.value })}>
        <option value="DIRECT">Слово → Перевод</option>
        <option value="REVERSE">Перевод → Слово</option>
        <option value="MIXED">Смешанный</option>
      </select>
      <div className="mt">
        <button className="btn primary lg" onClick={start}>
          ▶ Начать сеанс
        </button>
      </div>
    </div>
  );
}
