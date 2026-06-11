export type Lang = 'ru' | 'en';

const dict: Record<string, { ru: string; en: string }> = {
  dashboard: { ru: 'Главная', en: 'Dashboard' },
  dictionary: { ru: 'Мой словарь', en: 'My dictionary' },
  learn: { ru: 'Тренировка', en: 'Practice' },
  flash: { ru: '25-й кадр', en: 'Flash mode' },
  articles: { ru: 'Тексты', en: 'Texts' },
  profile: { ru: 'Профиль', en: 'Profile' },
  admin: { ru: 'Админка', en: 'Admin' },
  logout: { ru: 'Выйти', en: 'Log out' },
  login: { ru: 'Войти', en: 'Log in' },
  register: { ru: 'Регистрация', en: 'Sign up' },
  email: { ru: 'Email', en: 'Email' },
  password: { ru: 'Пароль', en: 'Password' },
  firstName: { ru: 'Имя', en: 'First name' },
  lastName: { ru: 'Фамилия', en: 'Last name' },
  forgotPassword: { ru: 'Забыли пароль?', en: 'Forgot password?' },
  know: { ru: 'Знаю', en: 'I know' },
  dontKnow: { ru: 'Не знаю', en: "I don't know" },
  showAnswer: { ru: 'Показать ответ', en: 'Show answer' },
  addWord: { ru: 'Добавить слово', en: 'Add word' },
  word: { ru: 'Слово', en: 'Word' },
  translation: { ru: 'Перевод', en: 'Translation' },
  example: { ru: 'Пример', en: 'Example' },
  delete: { ru: 'Удалить', en: 'Delete' },
  save: { ru: 'Сохранить', en: 'Save' },
  cancel: { ru: 'Отмена', en: 'Cancel' },
  premium: { ru: 'Premium', en: 'Premium' },
  getPremium: { ru: 'Оформить Premium', en: 'Get Premium' },
  learned: { ru: 'Выучено', en: 'Learned' },
  streak: { ru: 'Дней подряд', en: 'Day streak' },
  continueLearning: { ru: 'Продолжить обучение', en: 'Continue learning' },
  parseText: { ru: 'Разобрать свой текст', en: 'Parse your text' },
  addToDictionary: { ru: 'В словарь', en: 'Add to dictionary' },
  settings: { ru: 'Настройки', en: 'Settings' },
  deleteAccount: { ru: 'Удалить аккаунт', en: 'Delete account' },
  loading: { ru: 'Загрузка…', en: 'Loading…' },
};

let current: Lang = (localStorage.getItem('lang') as Lang) || 'ru';

export function setLang(lang: Lang) {
  current = lang;
  localStorage.setItem('lang', lang);
}

export function getLang(): Lang {
  return current;
}

export function t(key: string): string {
  return dict[key]?.[current] ?? key;
}
