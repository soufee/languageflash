import { FormEvent, useCallback, useEffect, useState } from 'react';
import api, { errorMessage } from '../api/client';

type Tab = 'users' | 'settings' | 'import' | 'articles';

interface AdminUser {
  id: number;
  email: string;
  firstName?: string;
  premium: boolean;
  premiumExpiresAt?: string;
  roles: string[];
  emailConfirmed: boolean;
}

interface Setting {
  key: string;
  value: string;
  type: string;
  description?: string;
}

interface ArticleRow {
  id: number;
  title: string;
  languageName: string;
  level: string;
}

export default function AdminPage() {
  const [tab, setTab] = useState<Tab>('users');
  return (
    <div className="grid">
      <h2 style={{ margin: 0 }}>Панель администратора</h2>
      <div className="row">
        {([['users', 'Пользователи'], ['settings', 'Настройки'], ['import', 'Импорт слов'], ['articles', 'Статьи']] as [Tab, string][]).map(([id, label]) => (
          <button key={id} className={`btn ${tab === id ? 'primary' : ''}`} onClick={() => setTab(id)}>
            {label}
          </button>
        ))}
      </div>
      {tab === 'users' && <UsersTab />}
      {tab === 'settings' && <SettingsTab />}
      {tab === 'import' && <ImportTab />}
      {tab === 'articles' && <ArticlesTab />}
    </div>
  );
}

function UsersTab() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [email, setEmail] = useState('');
  const [subscription, setSubscription] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const load = useCallback(() => {
    api.get('/admin/users', { params: { email: email || undefined, subscription: subscription || undefined, page } })
      .then((res) => { setUsers(res.data.content); setTotalPages(res.data.totalPages); });
  }, [email, subscription, page]);

  useEffect(() => { load(); }, [load]);

  async function togglePremium(u: AdminUser) {
    const expiresAt = u.premium ? null : new Date(Date.now() + 365 * 24 * 3600 * 1000).toISOString().slice(0, 19);
    await api.patch(`/admin/users/${u.id}`, { premium: !u.premium, premiumExpiresAt: expiresAt });
    load();
  }

  async function toggleBlock(u: AdminUser, blocked: boolean) {
    await api.patch(`/admin/users/${u.id}`, { blocked });
    load();
  }

  return (
    <div className="card">
      <div className="row">
        <input placeholder="Поиск по email" value={email} onChange={(e) => { setPage(0); setEmail(e.target.value); }} style={{ width: 240 }} />
        <select value={subscription} onChange={(e) => { setPage(0); setSubscription(e.target.value); }} style={{ width: 'auto' }}>
          <option value="">Все</option>
          <option value="FREE">FREE</option>
          <option value="PREMIUM">PREMIUM</option>
        </select>
      </div>
      <table className="mt">
        <thead>
          <tr><th>Email</th><th>Имя</th><th>Подписка</th><th>Действия</th></tr>
        </thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td>{u.email}{!u.emailConfirmed && ' ⏳'}</td>
              <td>{u.firstName}</td>
              <td>{u.premium ? `👑 до ${u.premiumExpiresAt ? new Date(u.premiumExpiresAt).toLocaleDateString() : '∞'}` : 'FREE'}</td>
              <td className="row">
                <button className="btn" onClick={() => togglePremium(u)}>
                  {u.premium ? 'Снять Premium' : 'Дать Premium (год)'}
                </button>
                <button className="btn danger" onClick={() => toggleBlock(u, true)}>Блок</button>
                <button className="btn" onClick={() => toggleBlock(u, false)}>Разблок</button>
              </td>
            </tr>
          ))}
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
  );
}

function SettingsTab() {
  const [settings, setSettings] = useState<Setting[]>([]);
  const [edits, setEdits] = useState<Record<string, string>>({});
  const [msg, setMsg] = useState('');

  useEffect(() => {
    api.get('/admin/settings').then((res) => setSettings(res.data));
  }, []);

  async function save() {
    const res = await api.patch('/admin/settings', edits);
    setSettings(res.data);
    setEdits({});
    setMsg('Сохранено');
    setTimeout(() => setMsg(''), 2000);
  }

  return (
    <div className="card">
      <table>
        <thead><tr><th>Ключ</th><th>Значение</th><th>Описание</th></tr></thead>
        <tbody>
          {settings.map((s) => (
            <tr key={s.key}>
              <td><code>{s.key}</code></td>
              <td>
                <input
                  value={edits[s.key] ?? s.value ?? ''}
                  onChange={(e) => setEdits({ ...edits, [s.key]: e.target.value })}
                  style={{ minWidth: 120 }}
                />
              </td>
              <td className="dim small">{s.description}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="row mt">
        <button className="btn primary" onClick={save} disabled={Object.keys(edits).length === 0}>Сохранить изменения</button>
        {msg && <span className="success-text">{msg}</span>}
      </div>
    </div>
  );
}

function ImportTab() {
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState('');
  const [error, setError] = useState('');

  async function upload(e: FormEvent) {
    e.preventDefault();
    if (!file) return;
    setResult(''); setError('');
    const data = new FormData();
    data.append('file', file);
    try {
      const res = await api.post('/admin/words/import', data, { headers: { 'Content-Type': 'multipart/form-data' } });
      setResult(`Импортировано: ${res.data.imported}, пропущено дубликатов: ${res.data.skipped}`);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="card">
      <h3>Импорт словаря из JSON</h3>
      <p className="dim small">
        Формат: {'{ "language": "English", "level": "B2", "words": [{ "word", "translation", "exampleSentence", "exampleTranslation", "tags": [] }] }'}
      </p>
      <form onSubmit={upload} className="row">
        <input type="file" accept=".json" onChange={(e) => setFile(e.target.files?.[0] ?? null)} style={{ width: 'auto' }} />
        <button className="btn primary" disabled={!file}>Загрузить</button>
      </form>
      {result && <div className="success-text">{result}</div>}
      {error && <div className="error-text">{error}</div>}
    </div>
  );
}

function ArticlesTab() {
  const [articles, setArticles] = useState<ArticleRow[]>([]);
  const [languages, setLanguages] = useState<{ id: number; name: string }[]>([]);
  const [editing, setEditing] = useState<any | null>(null);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api.get('/admin/articles', { params: { size: 50 } }).then((res) => setArticles(res.data.content));
    api.get('/languages').then((res) => setLanguages(res.data));
  }, []);

  useEffect(() => { load(); }, [load]);

  async function save(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      if (editing.id) await api.put(`/admin/articles/${editing.id}`, editing);
      else await api.post('/admin/articles', editing);
      setEditing(null);
      load();
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function remove(id: number) {
    await api.delete(`/admin/articles/${id}`);
    load();
  }

  if (editing) {
    return (
      <div className="card">
        <h3>{editing.id ? 'Редактирование статьи' : 'Новая статья'}</h3>
        <form onSubmit={save}>
          <label>Название</label>
          <input value={editing.title} onChange={(e) => setEditing({ ...editing, title: e.target.value })} required />
          <label>Текст</label>
          <textarea rows={10} value={editing.content} onChange={(e) => setEditing({ ...editing, content: e.target.value })} required />
          <label>Перевод (опционально)</label>
          <textarea rows={5} value={editing.translation ?? ''} onChange={(e) => setEditing({ ...editing, translation: e.target.value })} />
          <div className="row">
            <div>
              <label>Язык</label>
              <select value={editing.languageId} onChange={(e) => setEditing({ ...editing, languageId: +e.target.value })}>
                {languages.map((l) => <option key={l.id} value={l.id}>{l.name}</option>)}
              </select>
            </div>
            <div>
              <label>Уровень</label>
              <select value={editing.level} onChange={(e) => setEditing({ ...editing, level: e.target.value })}>
                {['A1', 'A2', 'B1', 'B2', 'C1', 'C2'].map((l) => <option key={l} value={l}>{l}</option>)}
              </select>
            </div>
          </div>
          {error && <div className="error-text">{error}</div>}
          <div className="row mt">
            <button className="btn primary">Сохранить</button>
            <button type="button" className="btn" onClick={() => setEditing(null)}>Отмена</button>
          </div>
        </form>
      </div>
    );
  }

  return (
    <div className="card">
      <button className="btn primary" onClick={() => setEditing({ title: '', content: '', languageId: languages[0]?.id, level: 'A1' })}>
        ＋ Новая статья
      </button>
      <table className="mt">
        <thead><tr><th>Название</th><th>Язык</th><th>Уровень</th><th /></tr></thead>
        <tbody>
          {articles.map((a) => (
            <tr key={a.id}>
              <td>{a.title}</td>
              <td>{a.languageName}</td>
              <td>{a.level}</td>
              <td className="row">
                <button className="btn" onClick={async () => {
                  const full = await api.get(`/articles/${a.id}`);
                  setEditing({ id: a.id, title: full.data.title, content: full.data.content, translation: full.data.translation, languageId: full.data.languageId, level: full.data.level });
                }}>✎</button>
                <button className="btn danger" onClick={() => remove(a.id)}>✕</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
