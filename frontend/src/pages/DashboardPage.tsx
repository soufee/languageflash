import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import { useAuth } from '../store/AuthContext';
import { t } from '../i18n';

interface Stats {
  totalWords: number;
  limit: number | null;
  unlimited: boolean;
  activeBatch: number;
  learnedTotal: number;
  learnedToday: number;
  learnedThisWeek: number;
  streakDays: number;
  learnedByDay: Record<string, number>;
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState<Stats | null>(null);
  const [days, setDays] = useState(7);

  useEffect(() => {
    api.get('/dashboard', { params: { days } }).then((res) => setStats(res.data));
  }, [days]);

  if (!stats) return <div className="dim">{t('loading')}</div>;

  const chartData = Object.entries(stats.learnedByDay);
  const max = Math.max(1, ...chartData.map(([, v]) => v));
  const limitPct = stats.limit ? Math.min(100, (stats.totalWords / stats.limit) * 100) : 0;

  return (
    <div className="grid">
      <h2 style={{ margin: 0 }}>
        Привет, {user?.firstName || 'друг'}! 👋
      </h2>
      {!user?.emailConfirmed && (
        <div className="card" style={{ borderColor: 'var(--warning)' }}>
          ⚠️ Подтвердите email, чтобы добавлять слова и проходить тренировки.{' '}
          <Link to="/confirm-email">Ввести код</Link>
        </div>
      )}
      <div className="grid cols-4">
        <div className="card">
          <div className="stat-value">
            {stats.totalWords}
            {stats.limit != null && <span style={{ fontSize: 18 }}> / {stats.limit}</span>}
          </div>
          <div className="stat-label">Слов в словаре{stats.unlimited && ' (безлимит 👑)'}</div>
          {stats.limit != null && (
            <div className="progress-bar mt">
              <div style={{ width: `${limitPct}%` }} />
            </div>
          )}
        </div>
        <div className="card">
          <div className="stat-value">{stats.activeBatch}</div>
          <div className="stat-label">В активной порции</div>
        </div>
        <div className="card">
          <div className="stat-value">{stats.learnedToday}</div>
          <div className="stat-label">{t('learned')} сегодня (за неделю: {stats.learnedThisWeek})</div>
        </div>
        <div className="card">
          <div className="stat-value">🔥 {stats.streakDays}</div>
          <div className="stat-label">{t('streak')}</div>
        </div>
      </div>

      <div className="grid cols-2">
        <div className="card">
          <h3>Быстрые действия</h3>
          <div className="row">
            <Link to="/learn" className="btn primary lg pulse">
              ▶ {t('continueLearning')}
            </Link>
            <Link to="/flash" className="btn lg">
              ⚡ {t('flash')}
            </Link>
            <Link to="/articles" className="btn lg">
              📖 {t('articles')}
            </Link>
          </div>
        </div>
        <div className="card">
          <div className="row spread">
            <h3 style={{ margin: 0 }}>Прогресс</h3>
            <select value={days} onChange={(e) => setDays(+e.target.value)} style={{ width: 'auto' }}>
              <option value={7}>7 дней</option>
              <option value={30}>30 дней</option>
            </select>
          </div>
          <div className="chart mt">
            {chartData.map(([date, value]) => (
              <div
                key={date}
                className="bar"
                style={{ height: `${(value / max) * 100}%` }}
                title={`${date}: ${value}`}
              />
            ))}
          </div>
          <div className="dim small mt">Выучено слов по дням · всего выучено: {stats.learnedTotal}</div>
        </div>
      </div>
    </div>
  );
}
