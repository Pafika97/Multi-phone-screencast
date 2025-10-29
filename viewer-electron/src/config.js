export const DEFAULT_SIGNAL_URL = "ws://localhost:8080";
export const DEFAULT_ROOM = "room1";
export const RTC_CONFIG = {
  iceServers: [
    { urls: ["stun:stun.l.google.com:19302"] },
    // Добавьте свой TURN для работы через NAT/интернет:
    // { urls: ["turn:your.turn.server:3478"], username: "user", credential: "pass" }
  ]
};