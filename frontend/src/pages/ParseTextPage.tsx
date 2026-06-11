import { useState } from 'react';
import api, { errorMessage } from '../api/client';
import TokenizedText, { Paragraph } from '../components/TokenizedText';

/** Разбор пользовательского текста «на лету» (ТЗ 3.2.2). */
export default function ParseTextPage() {
  const [text, setText] = useState('');
  const [sourceLanguage, setSourceLanguage] = useState('en');
  const [paragraphs, setParagraphs] = useState<Paragraph[] | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function parse() {
    setBusy(true);
    setError('');
    try {
      const res = await api.post('/articles/parse', {
        text,
        sourceLanguage,
        targetLanguage: 'ru',
      });
      setParagraphs(res.data.paragraphs);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="grid">
      <h2 style={{ margin: 0 }}>Разбор своего текста</h2>
      {!paragraphs ? (
        <div className="card">
          <label>Вставьте текст (до 10 000 символов)</label>
          <textarea
            rows={12}
            maxLength={10000}
            value={text}
            onChange={(e) => setText(e.target.value)}
            placeholder="Вставьте сюда статью или текст на изучаемом языке…"
          />
          <div className="dim small mt">{text.length} / 10000</div>
          <label>Язык текста</label>
          <select value={sourceLanguage} onChange={(e) => setSourceLanguage(e.target.value)} style={{ width: 'auto' }}>
            <option value="en">Английский</option>
            <option value="sr">Сербский</option>
            <option value="fr">Французский</option>
            <option value="de">Немецкий</option>
            <option value="es">Испанский</option>
          </select>
          {error && <div className="error-text">{error}</div>}
          <div className="mt">
            <button className="btn primary lg" onClick={parse} disabled={busy || text.trim().length === 0}>
              {busy ? 'Разбираем…' : 'Разобрать текст'}
            </button>
          </div>
        </div>
      ) : (
        <>
          <div>
            <button className="btn" onClick={() => setParagraphs(null)}>← Другой текст</button>
            <span className="dim small" style={{ marginLeft: 12 }}>кликните на слово для перевода</span>
          </div>
          <div className="card">
            <TokenizedText paragraphs={paragraphs} sourceLanguage={sourceLanguage} targetLanguage="ru" />
          </div>
        </>
      )}
    </div>
  );
}
