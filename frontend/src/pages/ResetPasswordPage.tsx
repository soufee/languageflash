import { FormEvent, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { errorMessage } from '../api/client';

export default function ResetPasswordPage() {
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [error, setError] = useState('');

  async function requestCode(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await api.post('/auth/reset-password/request', { email });
      setStep(2);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function confirm(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      await api.post('/auth/reset-password/confirm', { email, code, newPassword });
      setStep(3);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="form card">
      <h2>Восстановление пароля</h2>
      {step === 1 && (
        <form onSubmit={requestCode}>
          <label>Email</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          {error && <div className="error-text">{error}</div>}
          <div className="mt">
            <button className="btn primary">Отправить код</button>
          </div>
        </form>
      )}
      {step === 2 && (
        <form onSubmit={confirm}>
          <div className="dim small">Код отправлен на {email}</div>
          <label>Код из письма</label>
          <input value={code} onChange={(e) => setCode(e.target.value)} required />
          <label>Новый пароль (мин. 8 символов)</label>
          <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} minLength={8} required />
          {error && <div className="error-text">{error}</div>}
          <div className="mt">
            <button className="btn primary">Сменить пароль</button>
          </div>
        </form>
      )}
      {step === 3 && (
        <div>
          <div className="success-text">Пароль изменён!</div>
          <div className="mt">
            <Link to="/login" className="btn primary">
              Войти
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}
