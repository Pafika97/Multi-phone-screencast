import { WebSocketServer } from 'ws';
import { v4 as uuid } from 'uuid';

const PORT = process.env.PORT || 8080;
const wss = new WebSocketServer({ port: PORT });

// roomId => Map(clientId => ws)
const rooms = new Map();

function joinRoom(roomId, ws) {
  if (!rooms.has(roomId)) rooms.set(roomId, new Map());
  const id = uuid();
  rooms.get(roomId).set(id, ws);
  ws._roomId = roomId;
  ws._clientId = id;
  return id;
}

function leaveRoom(ws) {
  const roomId = ws._roomId;
  const clientId = ws._clientId;
  if (!roomId || !clientId) return;
  const room = rooms.get(roomId);
  if (!room) return;
  room.delete(clientId);
  if (room.size === 0) rooms.delete(roomId);
}

function broadcast(roomId, data, exceptId = null) {
  const room = rooms.get(roomId);
  if (!room) return;
  for (const [cid, client] of room.entries()) {
    if (cid === exceptId) continue;
    try { client.send(JSON.stringify(data)); } catch {}
  }
}

wss.on('connection', (ws) => {
  ws.on('message', (raw) => {
    let msg;
    try { msg = JSON.parse(raw); } catch { return; }
    const { type, roomId } = msg;

    if (type === 'join') {
      const id = joinRoom(roomId, ws);
      ws.send(JSON.stringify({ type: 'joined', clientId: id }));
      // сообщаем остальным о новом участнике
      broadcast(roomId, { type: 'peer-join', clientId: id }, id);
    }

    // сигнальные сообщения напрямую конкретному peer
    if (type === 'signal') {
      const { targetId, payload } = msg;
      const room = rooms.get(roomId);
      if (!room) return;
      const target = room.get(targetId);
      if (!target) return;
      try {
        target.send(JSON.stringify({ type: 'signal', fromId: ws._clientId, payload }));
      } catch {}
    }
  });

  ws.on('close', () => {
    const roomId = ws._roomId;
    const clientId = ws._clientId;
    if (roomId && clientId) {
      leaveRoom(ws);
      broadcast(roomId, { type: 'peer-leave', clientId }, null);
    }
  });
});

console.log(`Signaling server on ws://0.0.0.0:${PORT}`);