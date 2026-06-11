import { useEffect, useState } from 'react';
import api from '../api/client';
import { useAuth } from '../store/AuthContext';

/**
 * Рекламный блок для бесплатных пользователей (ТЗ 3.8).
 * При наличии ad_unit_id_web_banner сюда монтируется реальный код РСЯ/AdSense;
 * без него отображается заглушка-плейсхолдер.
 */
export default function AdBanner() {
  const { user } = useAuth();
  const [enabled, setEnabled] = useState(false);
  const [adUnitId, setAdUnitId] = useState('');

  useEffect(() => {
    api
      .get('/config')
      .then((res) => {
        setEnabled(res.data.adsEnabledWeb);
        setAdUnitId(res.data.adUnitIdWebBanner);
      })
      .catch(() => {});
  }, []);

  if (!enabled || user?.premium) return null;

  return (
    <div className="ad-banner" data-ad-unit={adUnitId || undefined}>
      Реклама · Отключите её с Premium-подпиской
    </div>
  );
}
