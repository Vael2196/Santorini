import { useEffect, useRef } from "react";
import { roomSound, SoundBank } from "./sounds";
import type { RoomState } from "./types";

export function useSounds(
  enabled: boolean,
  room: RoomState | null,
  status: string,
  error: string,
) {
  const bank = useRef<SoundBank | null>(null);
  if (!bank.current) bank.current = new SoundBank();
  const previous = useRef<RoomState | null>(null);
  const previousError = useRef("");
  useEffect(() => {
    bank.current!.setEnabled(enabled);
  }, [enabled]);
  useEffect(() => {
    if (status !== "online") {
      previous.current = null;
      return;
    }
    const sound = roomSound(previous.current, room);
    previous.current = room;
    if (sound) bank.current!.play(sound);
  }, [room, status]);
  useEffect(() => {
    if (error && error !== previousError.current) bank.current!.play("error");
    previousError.current = error;
  }, [error]);
  useEffect(() => () => bank.current!.stop(), []);
  return bank.current;
}
