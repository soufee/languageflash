import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../store/AuthContext';
import { t } from '../i18n';
import AdBanner from './AdBanner';

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="app">
      <header className="header">
        <div className="logo">⚡ Language Flash</div>
        <nav className="nav">
          <NavLink to="/">{t('dashboard')}</NavLink>
          <NavLink to="/dictionary">{t('dictionary')}</NavLink>
          <NavLink to="/learn">{t('learn')}</NavLink>
          <NavLink to="/flash">{t('flash')}</NavLink>
          <NavLink to="/articles">{t('articles')}</NavLink>
          {user?.roles.includes('ADMIN') && <NavLink to="/admin">{t('admin')}</NavLink>}
        </nav>
        <div className="row">
          {user && !user.premium && (
            <button className="btn primary" onClick={() => navigate('/paywall')}>
              ✨ Premium
            </button>
          )}
          {user?.premium && <span style={{ color: 'var(--warning)' }}>👑</span>}
          <NavLink to="/profile" className="dim small">
            {user?.firstName || user?.email}
          </NavLink>
          <button
            className="btn"
            onClick={() => {
              logout();
              navigate('/login');
            }}
          >
            {t('logout')}
          </button>
        </div>
      </header>
      <main className="container">
        <Outlet />
        <div className="mt">
          <AdBanner />
        </div>
      </main>
    </div>
  );
}
