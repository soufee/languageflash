import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './store/AuthContext';
import Layout from './components/Layout';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ConfirmEmailPage from './pages/ConfirmEmailPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import DashboardPage from './pages/DashboardPage';
import DictionaryPage from './pages/DictionaryPage';
import LearnPage from './pages/LearnPage';
import FlashPage from './pages/FlashPage';
import ArticlesPage from './pages/ArticlesPage';
import ArticleReadPage from './pages/ArticleReadPage';
import ParseTextPage from './pages/ParseTextPage';
import ProfilePage from './pages/ProfilePage';
import PaywallPage from './pages/PaywallPage';
import AdminPage from './pages/AdminPage';
import './styles.css';

function Protected({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="container dim">Загрузка…</div>;
  if (!user) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/confirm-email" element={<ConfirmEmailPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route
        element={
          <Protected>
            <Layout />
          </Protected>
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/dictionary" element={<DictionaryPage />} />
        <Route path="/learn" element={<LearnPage />} />
        <Route path="/flash" element={<FlashPage />} />
        <Route path="/articles" element={<ArticlesPage />} />
        <Route path="/articles/:id" element={<ArticleReadPage />} />
        <Route path="/parse" element={<ParseTextPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/paywall" element={<PaywallPage />} />
        <Route path="/admin" element={<AdminPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
