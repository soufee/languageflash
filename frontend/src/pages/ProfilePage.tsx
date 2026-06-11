import { FormEvent, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { errorMessage, setTokens } from '../api/client';
import { useAuth } from '../store/AuthContext';
import { setLang, getLang } from '../i18n';

export default function ProfilePage() {
  const { user, reload, logout } = useAuth();
  const navigate = useNavigate();
  const [profile, setProfile] = useState({ firstName: '', lastName: '', interfaceLanguage: 'ru' });
  const [passwords, setPasswords] = useState({ oldPassword: '', newPassword: '' });
  const [settings, setSettings] = useState<{ activeWordsCount: number; language: string | null; minLevel: string }>({ activeWordsCount: 50, language: null, minLevel: 'A1' });
  const [languages, setLanguages] = useState<{ id: number; name: string }[]>([]);
  const [msg, setMsg] = useState('');
  const [err, setErr] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(false);

  useEffect(() => {
    if (user) {
      setProfile({
        firstName: user.firstName ?? '',
        lastName: user.lastName ?? '',
        interfaceLanguage: user.interfaceLanguage,
      });
    }
    api.get('/user/me/settings').then((res) =>
      setSettings({
        activeWordsCount: res.data.activeWordsCount ?? 50,
        language: res.data.language ?? null,
        minLevel: res.data.minLevel ?? 'A1',
      })
    );
    api.get('/languages').then((res) => setLanguages(res.data));
  }, [user]);

  async function saveProfile(e: FormEvent) {
    e.preventDefault();
    setMsg(''); setErr('');
    try {
      await api.patch('/user/me', profile);
      setLang(profile.interfaceLanguage as any);
      await reload();
      setMsg('Профиль сохранён');
    } catch (e2) {
      setErr(errorMessage(e2));
    }
  }

  async function savePassword(e: FormEvent) {
    e.preventDefault();
    setMsg(''); setErr('');
    try {
      await api.put('/user/me/password', passwords);
      setPasswords({ oldPassword: '', newPassword: '' });
      setMsg('Пароль изменён');
    } catch (e2) {
      setErr(errorMessage(e2));
    }
  }

  async function saveSettings(e: FormEvent) {
    e.preventDefault();
    setMsg(''); setErr('');
    try {
      await api.put('/user/me/settings', settings);
      setMsg('Настройки обучения сохранены');
    } catch (e2) {
      setErr(errorMessage(e2));
    }
  }

  async function deleteAccount() {
    try {
      await api.delete('/user/me', { data: { confirmation: 'DELETE' } });
      setTokens(null, null);
      logout();
      navigate('/login');
    } catch (e2) {
      setErr(errorMessage(e2));
    }
  }

  return (
    <div className="grid cols-2">
      <div className="card">
        <h3>Профиль</h3>
        <div className="dim small">{user?.email} {user?.premium && '· Premium 👑'}</div>
        <form onSubmit={saveProfile}>
          <label>Имя</label>
          <input value={profile.firstName} onChange={(e) => setProfile({ ...profile, firstName: e.target.value })} />
          <label>Фамилия</label>
          <input value={profile.lastName} onChange={(e) => setProfile({ ...profile, lastName: e.target.value })} />
          <label>Язык интерфейса</label>
          <select value={profile.interfaceLanguage} onChange={(e) => setProfile({ ...profile, interfaceLanguage: e.target.value })}>
            <option value="ru">Русский</option>
            <option value="en">English</option>
          </select>
          <div className="mt"><button className="btn primary">Сохранить</button></div>
        </form>
      </div>

      <div className="card">
        <h3>Настройки обучения</h3>
        <form onSubmit={saveSettings}>
          <label>Размер активной порции: {settings.activeWordsCount} слов</label>
          <input type="range" min={10} max={100} step={5} value={settings.activeWordsCount}
                 onChange={(e) => setSettings({ ...settings, activeWordsCount: +e.target.value })} />
          <label>Изучаемый язык (для автодобора слов)</label>
          <select value={settings.language ?? ''} onChange={(e) => setSettings({ ...settings, language: e.target.value || null })}>
            <option value="">— не выбран —</option>
            {languages.map((l) => <option key={l.id} value={l.name}>{l.name}</option>)}
          </select>
          <label>Минимальный уровень</label>
          <select value={settings.minLevel} onChange={(e) => setSettings({ ...settings, minLevel: e.target.value })}>
            {['A1', 'A2', 'B1', 'B2', 'C1', 'C2'].map((l) => <option key={l} value={l}>{l}</option>)}
          </select>
          <div className="mt"><button className="btn primary">Сохранить</button></div>
        </form>
      </div>

      <div className="card">
        <h3>Смена пароля</h3>
        <form onSubmit={savePassword}>
          <label>Текущий пароль</label>
          <input type="password" value={passwords.oldPassword} onChange={(e) => setPasswords({ ...passwords, oldPassword: e.target.value })} required />
          <label>Новый пароль (мин. 8 символов)</label>
          <input type="password" value={passwords.newPassword} onChange={(e) => setPasswords({ ...passwords, newPassword: e.target.value })} minLength={8} required />
          <div className="mt"><button className="btn primary">Изменить пароль</button></div>
        </form>
      </div>

      <div className="card" style={{ borderColor: 'var(--danger)' }}>
        <h3 style={{ color: 'var(--danger)' }}>Опасная зона</h3>
        <p className="dim small">
          Удаление аккаунта необратимо: все персональные данные, словарь и прогресс будут стёрты (GDPR).
        </p>
        {!confirmDelete ? (
          <button className="btn danger" onClick={() => setConfirmDelete(true)}>Удалить аккаунт</button>
        ) : (
          <div className="row">
            <button className="btn danger" onClick={deleteAccount}>Да, удалить навсегда</button>
            <button className="btn" onClick={() => setConfirmDelete(false)}>Отмена</button>
          </div>
        )}
      </div>

      {(msg || err) && (
        <div className="card" style={{ gridColumn: '1 / -1' }}>
          {msg && <span className="success-text">{msg}</span>}
          {err && <span className="error-text">{err}</span>}
        </div>
      )}
    </div>
  );
}
