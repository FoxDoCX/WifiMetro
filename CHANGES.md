# WifiMetro — что изменено относительно старой сборки

База: fork [XeonDead/mosmetro-android](https://github.com/XeonDead/mosmetro-android)  
(он же поверх upstream [mosmetro-android/mosmetro-android](https://github.com/mosmetro-android/mosmetro-android)).

Текущая версия сборки: **2.6.1** (`versionCode` 811 / 812 для ABI-сплитов).

---

## 1. Версия и целевая платформа

| Параметр | Было (upstream / старый fork) | Стало |
|---|---|---|
| `versionName` | 2.3.2 | **2.6.1** |
| `versionCode` | 77 | **81** (+ суффикс ABI: v7 → `*10+1`, v8 → `*10+2`) |
| `compileSdk` | 31 → 35 (fork) | **36** (Android 16) |
| `targetSdk` | 25 → 30 (fork) | **36** |
| `minSdk` | 16 → 26 (fork) | **26** |
| ABI | universal / не зафиксировано | отдельные APK: **armeabi-v7a**, **arm64-v8a** |

Артефакты: `dist/WifiMetro-2.6.1-android16-*-debug.apk`

---

## 2. Критические багфиксы (падал / ломал работу)

### `Notify.java`
- **Было (fork):** в `show()` создавался пустой `new NotificationCompat.Builder(...).build()` — foreground/уведомления без контента.
- **Стало:** `super(context, CHANNEL_ID)` + нормальный `build()`, канал уведомлений, `ServiceCompat.startForeground(..., DATA_SYNC)`.

### `DnsClient.java`
- **Было (fork):** `InetSocketAddress.toString()` → строки вида `/1.1.1.1:53`, DNS ломался.
- **Стало:** берётся `getHostAddress()` / `getHostString()`.

### `WifiUtils.isEnabled()`
- **Было (fork):** вместо «Wi‑Fi включён» проверялся `SupplicantState` (association) — ложные «Wi‑Fi выключен».
- **Стало:** снова `WifiManager.isWifiEnabled()`; отдельно добавлен `isAssociated()`.

### `BackendWorker.java`
- **Было (fork):** `new Constraints(NetworkType.UNMETERED, false, false, false)` — несовместимо/хрупко на WorkManager 2.10.
- **Стало:** `Constraints.Builder`, ошибки старта не валят приложение.

### Краш при открытии на Android 15/16
- Тема **Holo** (`Theme.Holo.*`) заменена на **DeviceDefault**.
- `registerReceiver` без флагов → `Util.registerReceiver(..., EXPORTED/NOT_EXPORTED)`.
- Старт `MosMetroApp` обёрнут в try/catch (Logger / WorkManager / Sentry), чтобы побочный сбой не ронял UI.

---

## 3. Android 12–16 совместимость

- `android:exported` для Activity/Receiver с intent-filter.
- `POST_NOTIFICATIONS` + запрос при старте настроек (API 33+).
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_DATA_SYNC`.
- Сервисы: `foregroundServiceType="dataSync"` (ConnectionService, ReceiverService).
- Запуск сервисов через `ContextCompat.startForegroundService` (`Util.startService`).
- PendingIntent: всегда `FLAG_IMMUTABLE` (`Util.pendingFlags`).
- `ACCESS_FINE_LOCATION` (SSID на новых Android, иначе `<unknown ssid>`).
- `ACCESS_BACKGROUND_LOCATION` в манифесте.
- `WRITE_EXTERNAL_STORAGE` ограничен `maxSdkVersion=28`.
- Cleartext / network security: разрешён HTTP для captive portals (`wi-fi.ru`, `welcome.wi-fi.ru`, `auth.wi-fi.ru`, `gowifi.ru`, `mos.ru` и др.).
- Ранний `startForeground` в `ConnectionService.onStartCommand` (требование O+/FGS).

---

## 4. Сети / провайдеры (SSID)

Добавлено к списку поддержки:

- городской Wi‑Fi: `Moscow_WiFi_Free`, `Moscow_WIFI_Free`, `Moscow_Wi-Fi_Free`
- МТС: `MTS_WiFi_Free`, `MTS_WIFI_Free`
- сравнение SSID — **case-insensitive**

Уже было (транспорт Maximtelecom и др.):  
`MT_FREE`, `MosMetro_Free`, `MosGorTrans_Free`, `CPPK_Free`, `Air_WiFi_Free`, `AURA`, …  
+ ByFly (Беларусь) из fork.

> Важно: для `MTS_WiFi_Free` и части `Moscow_WiFi_Free` SSID добавлен,  
> но отдельный портал (МТС ID / Mos ID / SMS) может потребовать нового Provider —  
> автологин через `auth.wi-fi.ru` / `welcome.wi-fi.ru` работает там, где портал тот же.

---

## 5. Зависимости и сборка

Обновлено (по сравнению с upstream 2023; частично уже в XeonDead):

- Android Gradle Plugin **8.13**, Gradle wrapper **8.13**
- OkHttp, WorkManager, Sentry, dnsjava, jsoup, RecyclerView и др. — актуальные версии fork + наши правки
- Добавлен `gradlew` / `gradle/wrapper` (в исходном fork wrapper не было)
- `local.properties` для локального SDK (не коммитить секреты)
- ABI splits в `app/build.gradle`

---

## 6. Ключевые изменённые файлы

```
app/build.gradle
app/src/main/AndroidManifest.xml
app/src/main/res/xml/network_security_config.xml
app/src/main/res/values/themes.xml
app/src/main/res/values-v11/themes.xml
app/src/main/res/values-v14/themes.xml
app/src/main/res/values-v21/themes.xml

app/src/main/java/pw/thedrhax/util/Notify.java
app/src/main/java/pw/thedrhax/util/WifiUtils.java
app/src/main/java/pw/thedrhax/util/Util.java
app/src/main/java/pw/thedrhax/util/PermissionUtils.java
app/src/main/java/pw/thedrhax/mosmetro/httpclient/DnsClient.java
app/src/main/java/pw/thedrhax/mosmetro/MosMetroApp.java
app/src/main/java/pw/thedrhax/mosmetro/services/BackendWorker.java
app/src/main/java/pw/thedrhax/mosmetro/services/ConnectionService.java
app/src/main/java/pw/thedrhax/mosmetro/services/ReceiverService.java
app/src/main/java/pw/thedrhax/mosmetro/services/NetworkReceiver.java
app/src/main/java/pw/thedrhax/mosmetro/services/BootReceiver.java
app/src/main/java/pw/thedrhax/mosmetro/activities/SettingsActivity.java
app/src/main/java/pw/thedrhax/mosmetro/activities/DebugActivity.java
app/src/main/java/pw/thedrhax/mosmetro/activities/ConnectionServiceActivity.java
app/src/main/java/pw/thedrhax/mosmetro/widgets/ToggleWidget.java
app/src/main/java/pw/thedrhax/mosmetro/updater/UpdateChecker.java
app/src/main/java/pw/thedrhax/mosmetro/authenticator/Provider.java
app/src/main/java/pw/thedrhax/mosmetro/authenticator/ProviderMetrics.java
```

---

## 7. Чего ещё нет (осознанно)

- Полноценный silent-login для **МТС ID** / **Mos ID / SMS** без дампа captive portal.
- Release-подпись своим keystore (сейчас debug APK).
- PR обратно в upstream (репозиторий upstream фактически заморожен с 2023).

---

## 8. Как ставить

1. Удалить предыдущую debug-сборку (если была).
2. Установить `dist/WifiMetro-2.6.1-android16-arm64-v8a-debug.apk` (на большинстве телефонов).
3. Выдать разрешения: геолокация (SSID), уведомления; при необходимости — игнор оптимизации батареи.
4. Включить автоподключение; при проблемах на MIUI — «постоянный фоновый процесс».

Лицензия проекта по-прежнему **GPL-3.0**.
