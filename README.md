# WifiMetro (FoxDoC)

**WifiMetro** — Android-приложение для автоматической авторизации в бесплатных Wi‑Fi сетях общественного транспорта (Москва, Санкт-Петербург и другие города с провайдером МаксимаТелеком и партнёрами).

Сборка **FoxDoC** — форк с поддержкой **Android 16 (API 36)**, обновлённым UI, тёмной темой и ручным выбором сети.

| | |
|---|---|
| **Версия** | 2.8.4 (versionCode 88) |
| **Package** | `pw.thedrhax.mosmetro` |
| **minSdk** | 26 (Android 8.0) |
| **targetSdk** | 36 (Android 16) |
| **Лицензия** | [GPL-3.0](./LICENSE) |

## Установка

Скачайте APK под архитектуру вашего устройства из [раздела Releases](https://github.com/FoxDoCX/WifiMetro/releases):

| Архитектура | Файл |
|---|---|
| arm64 (большинство современных телефонов) | `WifiMetro-*-arm64-v8a-*.apk` |
| armeabi-v7a (старые 32-bit) | `WifiMetro-*-armeabi-v7a-*.apk` |

> Текущие сборки в Releases — **debug** (подписаны debug-ключом). Для release-сборки нужен локальный `keystore.properties`.

После установки запустите приложение один раз — оно будет отслеживать Wi‑Fi и подключаться к знакомым SSID автоматически.

## Возможности (FoxDoC 2.8.x)

- Автологин в сетях MT_FREE, Moscow_WiFi_Free, MTS_WiFi_Free, Aeroexpress_Free, CPPK_Free, BELTELECOM и др.
- **Меню «Wi‑Fi сети»** — ручной выбор SSID и принудительное подключение
- **Тёмная тема** — переключатель в ActionBar (настройки, отладка, обратная связь)
- Корректные отступы под статус-бар и навигацию (3-кнопочная и жестовая)
- Совместимость с Android 12–16 (FGS, exported-компоненты, edge-to-edge)

Полный changelog: [CHANGES.txt](./CHANGES.txt)

## Происхождение кода

| Репозиторий | Роль |
|---|---|
| [mosmetro-android/mosmetro-android](https://github.com/mosmetro-android/mosmetro-android) | Оригинальный upstream (GPL-3.0) |
| [XeonDead/mosmetro-android](https://github.com/XeonDead/mosmetro-android) | База форка (Android SDK, ByFly) |
| **FoxDoCX/WifiMetro** | Текущая сборка FoxDoC |

## Сборка из исходников

### Требования

- **JDK** 17+ (рекомендуется для AGP 8.13)
- **Android SDK** с platform **android-36**
- **Gradle** 8.13 (через wrapper в репозитории)

### Стек сборки

| Компонент | Версия |
|---|---|
| Gradle | 8.13 |
| Android Gradle Plugin | 8.13.0 |
| compileSdk / targetSdk | 36 |
| Java (source/target) | 10 |
| Desugar JDK libs | 2.1.5 |

### Зависимости (основные)

| Библиотека | Версия | Назначение |
|---|---|---|
| AndroidX RecyclerView | 1.4.0 | UI |
| AndroidX Work | 2.10.4 | фоновые задачи |
| OkHttp | 5.1.0 | HTTP-клиент |
| jsoup | 1.21.2 | парсинг HTML |
| dnsjava | 3.6.3 | DNS |
| Sentry Android | 8.21.1 | отчёты об ошибках |
| libsu | 3.1.2 | root-утилиты (опционально) |
| json-path | 2.9.0 | JSON |
| rangebar | 1.0.0 | UI-виджет |

### Команды

```bash
# Debug APK (ABI-split → app/build/outputs/apk/debug/)
./gradlew assembleDebug

# Release (нужен keystore.properties в корне проекта)
./gradlew assembleRelease
```

`keystore.properties` (не коммитить):

```properties
RELEASE_STORE_FILE=/path/to/keystore.jks
RELEASE_STORE_PASSWORD=...
RELEASE_KEY_ALIAS=...
RELEASE_KEY_PASSWORD=...
```

### ABI splits

Собираются отдельные APK:

- `armeabi-v7a` — versionCode override `871`
- `arm64-v8a` — versionCode override `872`

## Разработка

Импорт в **Android Studio** / IntelliJ IDEA. Локальный `local.properties` с путём к SDK создаётся автоматически IDE.

## Лицензия

Проект распространяется под **GNU GPL v3+**. См. [LICENSE](./LICENSE).

Исходный код предоставлен для ознакомления. Автор не несёт ответственности за использование приложения.
