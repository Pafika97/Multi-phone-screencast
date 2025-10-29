# WebRTC путь — краткая инструкция

1) **Signaling-сервер** (`signaling-server`):
```bash
npm i
node server.js
```
По умолчанию: `ws://0.0.0.0:8080`

2) **Viewer на Windows** (`viewer-electron`):
```bash
npm i
npm start
```
В окне введите URL сигналинга (например, `ws://<IP_ПК>:8080`) и `room1`.

3) **Android‑отправитель** (`android-sender`):
- Откройте папку `android-sender/` в Android Studio.
- Дождитесь синхронизации Gradle, соберите и установите APK.
- В приложении укажите URL сигналинга и ту же комнату.
- Нажмите **Start**, разрешите захват экрана.

> Для работы **через интернет/NAT** добавьте TURN‑сервер в `viewer-electron/src/config.js` и `RtcClient.kt`.