# Webhook System Changes

## Что изменилось

### Клиентская часть (WebhookHelper.java)
- ✅ Убрана вся логика получения geo-информации
- ✅ Клиент отправляет только базовые данные: Discord info, HWID, IP, hardware
- ✅ Все запросы асинхронные через `new Thread()`
- ✅ Отправляет hardware данные (CPU, GPU, RAM)

### Серверная часть (index.ts)
- ✅ Добавлена функция `fetchGeoLocation()` с fallback на два API:
  - Основной: `ip-api.com` (более детальный, определяет VPN/proxy)
  - Резервный: `ipapi.co` (если первый недоступен)
- ✅ Добавлена функция `getDiscordAvatarUrl()` для дефолтных аватарок Discord
- ✅ Сервер сам получает geo-данные по IP
- ✅ Сервер отправляет вебхуки в Discord

## Преимущества

1. **Безопасность**: Discord webhook URL не хранится в клиенте
2. **Надежность**: Два источника geo-данных с fallback
3. **Производительность**: Клиент не тратит время на geo-запросы
4. **Централизация**: Вся логика вебхуков на сервере
5. **Дефолтные аватарки**: Всегда показывается аватарка пользователя

## Деплой

```bash
cd onetap-auth
wrangler deploy
```

## Проверка работы

```bash
curl https://onetap-auth.wishen92.workers.dev/health
```

Должен вернуть: `{"status":"ok","timestamp":...}`

## Endpoints

- `POST /api/webhook/unauthorized` - Неавторизованный доступ
- `POST /api/webhook/startup` - Запуск клиента
- `POST /api/webhook/suspicious-command` - Подозрительная команда

Все вебхуки автоматически:
- Получают geo-данные по IP
- Определяют VPN/proxy
- Добавляют дефолтную аватарку Discord
- Отправляют в Discord webhook
