import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import api, { errorMessage } from '../api/client';
import { useAuth } from '../store/AuthContext';

interface BillingStatus {
  isPremium: boolean;
  expiresAt?: string;
  dictionaryUsed: number;
  dictionaryLimit: number | null;
  prices: { month1: number; month6: number; month12: number };
}

const reasons: Record<string, string> = {
  dictionary: 'Достигнут лимит бесплатного словаря',
  level: 'Уровни C1 и C2 доступны только с Premium',
};

export default function PaywallPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { reload } = useAuth();
  const [status, setStatus] = useState<BillingStatus | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const reason = params.get('reason');

  useEffect(() => {
    api.get('/billing/status').then((res) => setStatus(res.data));
  }, []);

  async function buy(plan: string) {
    setBusy(true);
    setError('');
    try {
      const checkout = await api.post('/billing/checkout', { plan });
      // мок-шлюз: сразу подтверждаем платёж; с боевым шлюзом здесь будет redirect на confirmationUrl
      await api.post('/billing/mock-confirm', { paymentId: checkout.data.paymentId });
      await reload();
      navigate('/');
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  if (!status) return <div className="dim">Загрузка…</div>;

  if (status.isPremium) {
    return (
      <div className="form card" style={{ textAlign: 'center' }}>
        <h2>👑 У вас уже есть Premium</h2>
        {status.expiresAt && <p className="dim">Действует до: {new Date(status.expiresAt).toLocaleDateString()}</p>}
      </div>
    );
  }

  const plans = [
    { id: 'PREMIUM_1M', label: '1 месяц', price: status.prices.month1, per: '' },
    { id: 'PREMIUM_6M', label: '6 месяцев', price: status.prices.month6, per: `≈${Math.round(status.prices.month6 / 6)} ₽/мес` },
    { id: 'PREMIUM_12M', label: '12 месяцев', price: status.prices.month12, per: `≈${Math.round(status.prices.month12 / 12)} ₽/мес` },
  ];

  return (
    <div className="grid" style={{ maxWidth: 760, margin: '0 auto' }}>
      <h2 style={{ textAlign: 'center' }}>✨ Language Flash Premium</h2>
      {reason && reasons[reason] && (
        <div className="card" style={{ borderColor: 'var(--warning)', textAlign: 'center' }}>
          {reasons[reason]}
        </div>
      )}
      <div className="card">
        <ul style={{ lineHeight: 2, margin: 0 }}>
          <li>🚫 Полное отключение рекламы</li>
          <li>📚 Безлимитный личный словарь (сейчас: {status.dictionaryUsed}{status.dictionaryLimit != null ? ` / ${status.dictionaryLimit}` : ''})</li>
          <li>🎓 Доступ к продвинутым уровням C1 и C2</li>
          <li>📲 Офлайн-режим в мобильном приложении (скоро)</li>
        </ul>
      </div>
      <div className="grid cols-2" style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
        {plans.map((p) => (
          <div key={p.id} className="card" style={{ textAlign: 'center' }}>
            <h3>{p.label}</h3>
            <div className="stat-value">{p.price} ₽</div>
            <div className="dim small">{p.per}</div>
            <button className="btn primary mt" onClick={() => buy(p.id)} disabled={busy}>
              Оформить
            </button>
          </div>
        ))}
      </div>
      {error && <div className="error-text">{error}</div>}
      <div className="dim small" style={{ textAlign: 'center' }}>
        Оплата производится через защищённый платёжный шлюз. Подписка продлевается вручную.
      </div>
    </div>
  );
}
