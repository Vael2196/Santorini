import { useEffect, useRef, useState } from "react";
import type { ChatMessage } from "./types";
export function Chat({
  messages,
  send,
  error,
  disabled,
  yourSeat,
}: {
  messages: ChatMessage[];
  send: (text: string) => void;
  error: string;
  disabled: boolean;
  yourSeat: number;
}) {
  const [draft, setDraft] = useState(""),
    [hidden, setHidden] = useState(false);
  const log = useRef<HTMLDivElement>(null),
    submitted = useRef("");
  useEffect(() => {
    if (log.current) log.current.scrollTop = log.current.scrollHeight;
  }, [messages, hidden]);
  useEffect(() => {
    if (error) setDraft((current) => current || submitted.current);
  }, [error]);
  return (
    <section className="paper-panel chat-panel" aria-label="Room chat">
      <div className="chat-heading">
        <h3>Room chat</h3>
        <button
          className="text-button"
          aria-expanded={!hidden}
          onClick={() => setHidden(!hidden)}
        >
          {hidden ? "Show chat" : "Hide chat"}
        </button>
      </div>
      {!hidden && (
        <>
          <div
            className="chat-log"
            ref={log}
            role="log"
            aria-label="Chat messages"
            aria-live="polite"
            aria-relevant="additions"
          >
            {!messages.length && (
              <p className="subtle">
                Messages stay in this room. Text only; no links.
              </p>
            )}
            {messages.map((m) => (
              <p key={m.id}>
                <strong className={`chat-seat-${m.seat}`}>
                  {m.name}
                  {m.seat === yourSeat ? " · you" : ""}
                </strong>{" "}
                <time>
                  {new Date(m.sentAt).toLocaleTimeString("en-GB", {
                    hour: "2-digit",
                    minute: "2-digit",
                  })}
                </time>
                <span>{m.text}</span>
              </p>
            ))}
          </div>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              if (!draft.trim() || disabled) return;
              submitted.current = draft;
              send(draft);
              setDraft("");
            }}
          >
            <label htmlFor="chat-message">Message</label>
            <input
              id="chat-message"
              maxLength={280}
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              autoComplete="off"
            />
            <div className="chat-heading">
              <small>{draft.length}/280</small>
              <button
                className="secondary"
                disabled={disabled || !draft.trim()}
              >
                Send message
              </button>
            </div>
          </form>
          {error && (
            <p className="notice" role="alert">
              {error}
            </p>
          )}
        </>
      )}
    </section>
  );
}
