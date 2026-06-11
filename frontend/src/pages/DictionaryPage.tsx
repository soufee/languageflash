import { FormEvent, useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { errorCode, errorMessage } from '../api/client';
import { t } from '../i18n';

interface Entry {
  id: number;
  word: string;
  translation: string;
  example?: string;
  source: string;
  learned: boolean;
  knowledgeFactor: number;
}

interface Status {
  used: number;
  limit: number | null;
  unlimited: boolean;
}

const sourceLabels: Record<string, string> = {
  SYSTEM: 'Из базы',
  ARTICLE: 'Из текста',
  MANUAL: 'Вручную',
};

export default function DictionaryPage() {
  const navigate = useNavigate();
  const [entries, setEntries] = useState<Entry[]>([]);
  const [status, setStatus] = useState<Status | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [filter, setFilter] = useState<{ source?: string; learned?: string }>({});
  const [form, setForm] = useState({ customWord: '', customTranslation: '', customExample: '' });
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    const params: any = { page, size: 20 };
    if (filter.source) params.source = filter.source;
    if (filter.learned) params.learned = filter.learned === 'true';
    const [list, st] = await Promise.all([
      api.get('/dictionary', { params }),
      api.get('/dictionary/status'),
    ]);
    setEntries(list.data.content);
    setTotalPages(list.data.totalPages);
    setStatus(st.data);
  }, [page, filter]);

  useEffect(() => {
    load();
  }, [load]);

  async function addWord(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await api.post('/dictionary', { ...form, source: 'MANUAL' });
      setForm({ customWord: '', customTranslation: '', customExample: '' });
      load();
    } catch (err) {
      if (errorCode(err) === 'DICTIONARY_LIMIT_REACHED') {
        navigate('/paywall?reason=dictionary');
      } else {
        setError(errorMessage(err));
      }
    }
  }

  async function remove(id: number) {
    await api.delete(`/dictionary/${id}`);
    load();
  }

  return (
    <div className="grid">
      <div className="row spread">
        <h2 style={{ margin: 0 }}>{t('dictionary')}</h2>
        {status && (
          <div className="card" style={{ padding: '10px 18px' }}>
            Использовано:{' '}
            <b>
              {status.used}
              {status.limit != null ? ` / ${status.limit}` : ' (безлимит 👑)'}
            </b>
          </div>
        )}
      </div>

      <div className="card">
        <h3>{t('addWord')}</h3>
        <form onSubmit={addWord} className="row" style={{ alignItems: 'flex-end' }}>
          <div style={{ flex: 1, minWidth: 160 }}>
            <label>{t('word')}</label>
            <input value={form.customWord} onChange={(e) => setForm({ ...form, customWord: e.target.value })} required maxLength={100} />
          </div>
          <div style={{ flex: 1, minWidth: 160 }}>
            <label>{t('translation')}</label>
            <input value={form.customTranslation} onChange={(e) => setForm({ ...form, customTranslation: e.target.value })} required maxLength={200} />
          </div>
          <div style={{ flex: 2, minWidth: 200 }}>
            <label>{t('example')}</label>
            <input value={form.customExample} onChange={(e) => setForm({ ...form, customExample: e.target.value })} maxLength={500} />
          </div>
          <button className="btn primary">+</button>
        </form>
        {error && <div className="error-text">{error}</div>}
      </div>

      <div className="card">
        <div className="row">
          <select value={filter.source ?? ''} onChange={(e) => { setPage(0); setFilter({ ...filter, source: e.target.value || undefined }); }} style={{ width: 'auto' }}>
            <option value="">Все источники</option>
            <option value="SYSTEM">Из базы</option>
            <option value="ARTICLE">Из текстов</option>
            <option value="MANUAL">Вручную</option>
          </select>
          <select value={filter.learned ?? ''} onChange={(e) => { setPage(0); setFilter({ ...filter, learned: e.target.value || undefined }); }} style={{ width: 'auto' }}>
            <option value="">Все слова</option>
            <option value="false">Изучаются</option>
            <option value="true">Выученные</option>
          </select>
        </div>
        <table className="mt">
          <thead>
            <tr>
              <th>{t('word')}</th>
              <th>{t('translation')}</th>
              <th>Источник</th>
              <th>Статус</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {entries.map((e) => (
              <tr key={e.id}>
                <td><b>{e.word}</b></td>
                <td>{e.translation}</td>
                <td className="dim small">{sourceLabels[e.source] ?? e.source}</td>
                <td>{e.learned ? '✅ Выучено' : `📚 ${Math.round((1 - Math.min(e.knowledgeFactor, 1)) * 100)}%`}</td>
                <td>
                  <button className="btn danger" onClick={() => remove(e.id)}>✕</button>
                </td>
              </tr>
            ))}
            {entries.length === 0 && (
              <tr><td colSpan={5} className="dim">Словарь пуст — добавьте слова вручную, из текстов или из общей базы</td></tr>
            )}
          </tbody>
        </table>
        {totalPages > 1 && (
          <div className="row mt">
            <button className="btn" disabled={page === 0} onClick={() => setPage(page - 1)}>←</button>
            <span className="dim">{page + 1} / {totalPages}</span>
            <button className="btn" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>→</button>
          </div>
        )}
      </div>
    </div>
  );
}
