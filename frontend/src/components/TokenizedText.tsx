import { useCallback, useRef, useState } from 'react';
import api, { errorCode } from '../api/client';
import { useNavigate } from 'react-router-dom';

export interface Token {
  text: string;
  isWord: boolean;
}
export interface Sentence {
  text: string;
  tokens: Token[];
}
export interface Paragraph {
  sentences: Sentence[];
}

interface TooltipState {
  x: number;
  y: number;
  text: string;
  translation: string | null;
  loading: boolean;
  added: boolean;
}

/**
 * Интерактивный текст (ТЗ 3.2): клик по слову — тултип с переводом
 * и кнопкой «Добавить в словарь».
 */
export default function TokenizedText({
  paragraphs,
  sourceLanguage,
  targetLanguage,
}: {
  paragraphs: Paragraph[];
  sourceLanguage: string;
  targetLanguage: string;
}) {
  const [tooltip, setTooltip] = useState<TooltipState | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  const onWordClick = useCallback(
    async (e: React.MouseEvent, text: string) => {
      e.stopPropagation();
      const rect = containerRef.current!.getBoundingClientRect();
      const x = e.clientX - rect.left;
      const y = e.clientY - rect.top + 24;
      setTooltip({ x, y, text, translation: null, loading: true, added: false });
      try {
        const res = await api.post('/articles/translate', {
          text,
          sourceLanguage,
          targetLanguage,
        });
        setTooltip((t) =>
          t && t.text === text
            ? { ...t, translation: res.data.available ? res.data.translation : null, loading: false }
            : t
        );
      } catch {
        setTooltip((t) => (t && t.text === text ? { ...t, loading: false } : t));
      }
    },
    [sourceLanguage, targetLanguage]
  );

  async function addToDictionary() {
    if (!tooltip) return;
    try {
      await api.post('/dictionary', {
        customWord: tooltip.text,
        customTranslation: tooltip.translation ?? '',
        source: 'ARTICLE',
      });
      setTooltip({ ...tooltip, added: true });
    } catch (err) {
      if (errorCode(err) === 'DICTIONARY_LIMIT_REACHED') {
        navigate('/paywall?reason=dictionary');
      }
    }
  }

  return (
    <div ref={containerRef} style={{ position: 'relative' }} onClick={() => setTooltip(null)}>
      <div className="article-text">
        {paragraphs.map((p, pi) => (
          <p key={pi}>
            {p.sentences.map((s, si) => (
              <span key={si} className="sentence">
                {s.tokens.map((tok, ti) =>
                  tok.isWord ? (
                    <span key={ti} className="tok" onClick={(e) => onWordClick(e, tok.text)}>
                      {tok.text}{' '}
                    </span>
                  ) : (
                    <span key={ti}>{tok.text} </span>
                  )
                )}
              </span>
            ))}
          </p>
        ))}
      </div>
      {tooltip && (
        <div className="tooltip" style={{ left: tooltip.x, top: tooltip.y }} onClick={(e) => e.stopPropagation()}>
          <b>{tooltip.text}</b>
          <div className="mt">
            {tooltip.loading
              ? '…'
              : tooltip.translation ?? 'Перевод временно недоступен — добавьте слово вручную'}
          </div>
          {tooltip.translation && (
            <div className="mt">
              {tooltip.added ? (
                <span className="success-text">✓ Добавлено</span>
              ) : (
                <button className="btn primary" onClick={addToDictionary}>
                  ＋ В словарь
                </button>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
