import { FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api, { errorMessage } from '../api/client';
import { t } from '../i18n';

export default function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: '', password: '', firstName: '', lastName: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await api.post('/auth/register', form);
      navigate('/confirm-email?email=' + encodeURIComponent(form.email));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  const set = (k: string) => (e: any) => setForm({ ...form, [k]: e.target.value });

  return (
    <div className="form card">
      <h2>{t('register')}</h2>
      <form onSubmit={onSubmit}>
        <label>{t('email')}</label>
        <input type="email" value={form.email} onChange={set('email')} required />
        <label>{t('password')} (мин. 8 символов)</label>
        <input type="password" value={form.password} onChange={set('password')} minLength={8} required />
        <label>{t('firstName')}</label>
        <input value={form.firstName} onChange={set('firstName')} />
        <label>{t('lastName')}</label>
        <input value={form.lastName} onChange={set('lastName')} />
        {error && <div className="error-text">{error}</div>}
        <div className="mt row">
          <button className="btn primary" disabled={busy}>
            {t('register')}
          </button>
          <Link to="/login" className="btn">
            {t('login')}
          </Link>
        </div>
      </form>
    </div>
  );
}
