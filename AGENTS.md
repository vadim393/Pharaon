# Pharaon — совместная разработка

## Требования
- JDK 21 (обязательно, сборка идёт на `--release 21`)
- Git
- IntelliJ IDEA (для совместного кодинга)

## Команды
Windows (PowerShell):
```
.\gradlew.bat compileJava          # быстрая проверка компиляции
.\gradlew.bat runClient            # запуск клиента Minecraft
.\gradlew.bat build                # полная сборка (jar в build/libs)
```

macOS / Linux:
```
./gradlew compileJava
./gradlew runClient
./gradlew build
```

## Git workflow
- Перед началом работы всегда: `git pull`
- После изменений:
  ```
  git add -A
  git commit -m "описание изменения"
  git push
  ```
- Не коммитить: `build/`, `.gradle/`, `run/`, `.idea/`, логи (`*.log`, `*.txt` в корне), `net/`, `.freebuff/` — всё это уже в `.gitignore`
- Работайте в разных файлах, если это возможно, чтобы избежать конфликтов
- Если конфликт всё же возник — не форсите push (`git push --force` запрещён), лучше решите, что оставить

## Совместный кодинг в реальном времени (Code Together)
1. Оба: IDEA → `Settings → Plugins → Marketplace` → установи **Code Together** → Restart
2. Ты: `Tools → Code Together → Start New Session` → скопируй ссылку другу
3. Друг: открывает ссылку → присоединяется
4. Перед сессией оба делают `git pull`, чтобы быть на одной версии

## onetap-auth
- Отдельная папка с Cloudflare Workers (аутентификация). Deploy отдельно:
  ```
  cd onetap-auth
  npm install
  npx wrangler deploy
  ```
- Секреты/токены НЕ коммитить. Проверяй перед коммитом: `git status`.