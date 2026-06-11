import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';

interface ArticleSummary {
  id: number;
  title: string;
  languageName: string;
  level: string;
  tags: { name: string; russianName: string; color: string }[];
}

interface Language {
  id: number;
  name: string;
}

export default function ArticlesPage() {
  const [articles, setArticles] = useState<ArticleSummary[]>([]);
  const [languages, setLanguages] = useState<Language[]>([]);
  const [filters, setFilters] = useState<{ languageId?: string; level?: string }>({});
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    api.get('/languages').then((res) => setLanguages(res.data));
  }, []);

  useEffect(() => {
    const params: any = { page, size: 20 };
    if (filters.languageId) params.languageId = filters.languageId;
    if (filters.level) params.level = filters.level;
    api.get('/articles', { params }).then((res) => {
      setArticles(res.data.content);
      setTotalPages(res.data.totalPages);
    });
  }, [filters, page]);

  return (
    <div className="grid">
      <div className="row spread">
        <h2 style={{ margin: 0 }}>📖 Библиотека текстов</h2>
        <Link to="/parse" className="btn primary">
          ＋ Разобрать свой текст
        </Link>
      </div>
      <div className="card">
        <div className="row">
          <select value={filters.languageId ?? ''} onChange={(e) => { setPage(0); setFilters({ ...filters, languageId: e.target.value || undefined }); }} style={{ width: 'auto' }}>
            <option value="">Все языки</option>
            {languages.map((l) => (
              <option key={l.id} value={l.id}>{l.name}</option>
            ))}
          </select>
          <select value={filters.level ?? ''} onChange={(e) => { setPage(0); setFilters({ ...filters, level: e.target.value || undefined }); }} style={{ width: 'auto' }}>
            <option value="">Все уровни</option>
            {['A1', 'A2', 'B1', 'B2', 'C1', 'C2'].map((l) => (
              <option key={l} value={l}>{l}{(l === 'C1' || l === 'C2') ? ' 👑' : ''}</option>
            ))}
          </select>
        </div>
      </div>
      <div className="grid cols-2">
        {articles.map((a) => (
          <Link to={`/articles/${a.id}`} key={a.id} className="card" style={{ color: 'inherit' }}>
            <h3>{a.title}</h3>
            <div className="row">
              <span className="tag" style={{ borderColor: 'var(--accent2)', color: 'var(--accent2)' }}>
                {a.languageName} · {a.level}
              </span>
              {a.tags.map((t) => (
                <span key={t.name} className="tag" style={{ borderColor: t.color, color: t.color }}>
                  {t.russianName}
                </span>
              ))}
            </div>
          </Link>
        ))}
        {articles.length === 0 && <div className="dim">Текстов пока нет</div>}
      </div>
      {totalPages > 1 && (
        <div className="row">
          <button className="btn" disabled={page === 0} onClick={() => setPage(page - 1)}>←</button>
          <span className="dim">{page + 1} / {totalPages}</span>
          <button className="btn" disabled={page >= totalPages - 1} onClick={() => setPage(page + 1)}>→</button>
        </div>
      )}
    </div>
  );
}
