import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api, { errorCode } from '../api/client';
import TokenizedText, { Paragraph } from '../components/TokenizedText';

interface ArticleFull {
  id: number;
  title: string;
  languageName: string;
  level: string;
  parsed: Paragraph[];
  translation?: string;
}

const langCodes: Record<string, string> = {
  English: 'en',
  Serbian: 'sr',
  French: 'fr',
  German: 'de',
  Spanish: 'es',
  Italian: 'it',
};

export default function ArticleReadPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [article, setArticle] = useState<ArticleFull | null>(null);
  const [showTranslation, setShowTranslation] = useState(false);

  useEffect(() => {
    api
      .get(`/articles/${id}`)
      .then((res) => setArticle(res.data))
      .catch((err) => {
        if (errorCode(err) === 'PREMIUM_REQUIRED') navigate('/paywall?reason=level');
      });
  }, [id, navigate]);

  if (!article) return <div className="dim">Загрузка…</div>;

  return (
    <div className="grid">
      <div className="row spread">
        <h2 style={{ margin: 0 }}>{article.title}</h2>
        <Link to="/articles" className="btn">← К списку</Link>
      </div>
      <div className="dim small">
        {article.languageName} · {article.level} · кликните на слово, чтобы увидеть перевод
      </div>
      <div className="card">
        <TokenizedText
          paragraphs={article.parsed}
          sourceLanguage={langCodes[article.languageName] ?? 'en'}
          targetLanguage="ru"
        />
      </div>
      {article.translation && (
        <div className="card">
          <button className="btn" onClick={() => setShowTranslation(!showTranslation)}>
            {showTranslation ? 'Скрыть перевод' : 'Показать перевод текста'}
          </button>
          {showTranslation && <p className="mt dim">{article.translation}</p>}
        </div>
      )}
    </div>
  );
}
