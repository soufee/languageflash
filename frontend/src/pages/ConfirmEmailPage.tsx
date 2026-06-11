import { FormEvent, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import api, { errorMessage } from '../api/client';

export default function ConfirmEmailPage() {
  const [params] = useSearchParams();
  const [email, setEmail] = useState(params.get('email') ?? '');
  const [code, setCode] = useState(params.get('code') ?? '');
  const [status, setStatus] = useState<'idle' | 'ok' | 'error'>('idle');
  const [error, setError] = useState('');

  async function confirm(em: string, cd: string) {
    try {
      await api.post('/auth/confirm-email', { email: em, code: cd });
      setStatus('ok');
    } catch (err) {
      setError(errorMessage(err));
      setStatus('error');
    }
  }

  useEffect(() => {
    const em = params.get('email');
    const cd = params.get('code');
    if (em && cd) confirm(em, cd);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    confirm(email, code);
  }

  return (
    <div className="form card">
      <h2>Подтверждение email</h2>
      {status === 'ok' ? (
        <div>
          <div className="success-text">Email подтверждён!</div>
          <div className="mt">
            <Link to="/login" className="btn primary">
              Войти
            </Link>
          </div>
        </div>
      ) : (
        <form onSubmit={onSubmit}>
          <label>Email</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <label>Код из письма</label>
          <input value={code} onChange={(e) => setCode(e.target.value)} required />
          {error && <div className="error-text">{error}</div>}
          <div className="mt">
            <button className="btn primary">Подтвердить</button>
          </div>
        </form>
      )}
    </div>
  );
}
