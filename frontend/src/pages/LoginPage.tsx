import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../store/AuthContext';
import { errorMessage } from '../api/client';
import { t } from '../i18n';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(email, password);
      navigate('/');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="form card">
      <h2>⚡ Language Flash</h2>
      <form onSubmit={onSubmit}>
        <label>{t('email')}</label>
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <label>{t('password')}</label>
        <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        {error && <div className="error-text">{error}</div>}
        <div className="mt row">
          <button className="btn primary" disabled={busy}>
            {t('login')}
          </button>
          <Link to="/register" className="btn">
            {t('register')}
          </Link>
        </div>
      </form>
      <div className="mt small">
        <Link to="/reset-password">{t('forgotPassword')}</Link>
      </div>
      <div className="mt small dim">
        Вход через Google / VK / Apple появится после настройки ключей провайдеров.
      </div>
    </div>
  );
}
