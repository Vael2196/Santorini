import { useCallback, useEffect, useRef, useState } from "react";
import type {
  ChatMessage,
  Command,
  Identity,
  RoomState,
  ServerEvent,
} from "./types";

const TOKEN_KEY = "santorini.session.v1";
export function useGame() {
  const [identity, setIdentity] = useState<Identity | null>(null);
  const bye = useRef<(() => void) | null>(null);
  const [status, setStatus] = useState("offline");
  const [room, setRoom] = useState<RoomState | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [chatError, setChatError] = useState("");
  const [chatBusy, setChatBusy] = useState(false);
  const roomId = useRef<string | null>(null);
  const chatPending = useRef<{
    id: string;
    timer: ReturnType<typeof setTimeout>;
  } | null>(null);
  const clearChat = () => {
    if (chatPending.current) clearTimeout(chatPending.current.timer);
    chatPending.current = null;
    setChatBusy(false);
  };
  const resetChat = () => {
    roomId.current = null;
    setMessages([]);
    setChatError("");
    clearChat();
  };
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const socket = useRef<WebSocket | null>(null);
  const token = useRef(sessionStorage.getItem(TOKEN_KEY));
  const wanted = useRef(false);
  const retry = useRef<ReturnType<typeof setTimeout> | null>(null);
  const pending = useRef<ReturnType<typeof setTimeout> | null>(null);
  const attempts = useRef(0);
  const name = useRef("");
  const dial = useRef<() => void>(() => {});

  const clearPending = useCallback(() => {
    if (pending.current) clearTimeout(pending.current);
    setBusy(false);
  }, []);

  dial.current = () => {
    if (!wanted.current) return;
    setStatus(token.current ? "reconnecting" : "connecting");
    const ws = new WebSocket(
      `${location.protocol === "https:" ? "wss:" : "ws:"}//${location.host}/ws`,
    );
    socket.current = ws;
    ws.onopen = () => {
      if (socket.current !== ws) return;
      ws.send(
        JSON.stringify({
          type: "HELLO",
          token: token.current,
          name: name.current,
        }),
      );
    };
    ws.onmessage = ({ data }) => {
      if (socket.current !== ws) return;
      try {
        const event: ServerEvent = JSON.parse(data);
        if (event.type === "WELCOME" && event.token) {
          setIdentity(event.identity);
          token.current = event.token;
          sessionStorage.setItem(TOKEN_KEY, event.token);
          attempts.current = 0;
          setStatus("online");
          setError("");
        } else if (event.type === "STATE" && event.room) {
          if (roomId.current !== event.room.id) {
            resetChat();
            roomId.current = event.room.id;
          }
          setRoom((previous) =>
            previous?.id === event.room!.id &&
            previous.version > event.room!.version
              ? previous
              : event.room,
          );
          clearPending();
        } else if (
          event.type === "CHAT_HISTORY" &&
          event.roomId === roomId.current
        ) {
          setMessages((event.history ?? []).slice(-50));
        } else if (
          event.type === "CHAT" &&
          event.roomId === roomId.current &&
          event.chat
        ) {
          setMessages((previous) =>
            [
              ...previous.filter((m) => m.id !== event.chat!.id),
              event.chat!,
            ].slice(-50),
          );
          if (event.requestId === chatPending.current?.id) clearChat();
        } else if (event.type === "BYE") {
          wanted.current = false;
          token.current = null;
          sessionStorage.removeItem(TOKEN_KEY);
          setRoom(null);
          resetChat();
          setIdentity(null);
          setStatus("offline");
          bye.current?.();
          bye.current = null;
          ws.close();
        } else if (event.type === "LEFT") {
          setRoom(null);
          resetChat();
          clearPending();
          if (event.message) setError(event.message);
        } else if (event.type === "ERROR") {
          if (event.requestId && event.requestId === chatPending.current?.id) {
            clearChat();
            setChatError(event.message ?? "Your message could not be sent.");
            return;
          }
          clearPending();
          setError(event.message ?? "Something went wrong.");
          if (event.code === "SESSION_EXPIRED") {
            token.current = null;
            sessionStorage.removeItem(TOKEN_KEY);
            wanted.current = false;
            setRoom(null);
            resetChat();
            ws.close();
            setStatus("offline");
          } else if (!token.current) {
            wanted.current = false;
            ws.close();
          }
        }
      } catch {
        setError(
          "An unreadable update arrived. Reconnect to refresh your board.",
        );
      }
    };
    ws.onclose = () => {
      if (socket.current !== ws) return;
      clearPending();
      clearChat();
      if (!wanted.current) {
        setStatus("offline");
        return;
      }
      setStatus("reconnecting");
      retry.current = setTimeout(
        () => dial.current(),
        Math.min(5000, 1000 * ++attempts.current),
      );
    };
    ws.onerror = () => {
      setError("Connection interrupted. Trying to reconnect…");
    };
  };

  useEffect(() => {
    if (token.current) {
      wanted.current = true;
      dial.current();
    }
    const heartbeat = setInterval(() => {
      if (socket.current?.readyState === WebSocket.OPEN)
        socket.current.send(JSON.stringify({ type: "PING" }));
    }, 20_000);
    return () => {
      wanted.current = false;
      if (retry.current) clearTimeout(retry.current);
      if (pending.current) clearTimeout(pending.current);
      if (chatPending.current) clearTimeout(chatPending.current.timer);
      clearInterval(heartbeat);
      const ws = socket.current;
      socket.current = null;
      ws?.close();
    };
  }, []);

  const connect = (displayName: string) => {
    if (status !== "offline") return;
    name.current = displayName.trim();
    wanted.current = true;
    setError("");
    dial.current();
  };
  const send = (command: Command) => {
    if (
      status !== "online" ||
      busy ||
      socket.current?.readyState !== WebSocket.OPEN
    )
      return;
    setError("");
    setBusy(true);
    socket.current.send(
      JSON.stringify({
        ...command,
        requestId: crypto.randomUUID(),
        version: room?.version,
      }),
    );
    pending.current = setTimeout(() => {
      setBusy(false);
      setError("The update took too long. Reconnecting to refresh your board.");
      socket.current?.close();
    }, 8000);
  };
  const disconnect = async () => {
    wanted.current = false;
    if (retry.current) clearTimeout(retry.current);
    if (socket.current?.readyState === WebSocket.OPEN) {
      await new Promise<void>((resolve) => {
        const timeout = setTimeout(resolve, 1500);
        bye.current = () => {
          clearTimeout(timeout);
          resolve();
        };
        socket.current!.send(JSON.stringify({ type: "GOODBYE" }));
      });
    }
    socket.current?.close();
    socket.current = null;
    token.current = null;
    sessionStorage.removeItem(TOKEN_KEY);
    setRoom(null);
    resetChat();
    setIdentity(null);
    setStatus("offline");
    setError("");
    clearPending();
  };
  const sendChat = (text: string) => {
    if (
      status !== "online" ||
      !room ||
      chatPending.current ||
      socket.current?.readyState !== WebSocket.OPEN
    )
      return;
    const id = crypto.randomUUID();
    setChatError("");
    setChatBusy(true);
    chatPending.current = {
      id,
      timer: setTimeout(() => {
        clearChat();
        setChatError("Your message took too long to send. Please try again.");
      }, 8000),
    };
    socket.current.send(JSON.stringify({ type: "CHAT", requestId: id, text }));
  };
  return {
    status,
    room,
    identity,
    error,
    busy,
    connect,
    disconnect,
    send,
    messages,
    chatError,
    chatBusy,
    sendChat,
  };
}
