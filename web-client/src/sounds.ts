import type { RoomState } from "./types";

const clips = {
  click: 0.3,
  select: 0.3,
  ready: 0.25,
  error: 0.2,
  move: 0.35,
  place: 0.3,
  build: 0.23,
  book: 0.25,
  finish: 0.25,
} as const;
export type Sound = keyof typeof clips;

export class SoundBank {
  private volume = 0.8;
  private enabled = true;
  private unlocked = false;
  private voices = new Map<Sound, HTMLAudioElement>();
  constructor(private createAudio = (src: string) => new Audio(src)) {}
  unlock() {
    this.unlocked = true;
  }
  setEnabled(enabled: boolean) {
    this.enabled = enabled;
    if (!enabled) this.stop();
  }
  setVolume(percent: number) {
    this.volume = Math.max(0, Math.min(100, percent)) / 100;
    for (const [sound, voice] of this.voices)
      voice.volume = clips[sound] * this.volume;
    if (!this.volume) this.stop();
  }
  play(sound: Sound) {
    if (
      !this.enabled ||
      !this.volume ||
      !this.unlocked ||
      document.visibilityState === "hidden"
    )
      return;
    try {
      let voice = this.voices.get(sound);
      if (!voice) {
        voice = this.createAudio(`/assets/audio/${sound}.mp3`);
        voice.volume = clips[sound] * this.volume;
        this.voices.set(sound, voice);
      }
      voice.currentTime = 0;
      // Playback may be blocked by browser policy or an unavailable output device.
      void voice.play().catch(() => {});
    } catch {
      /* Audio must never interrupt a game action. */
    }
  }
  stop() {
    for (const voice of this.voices.values()) {
      voice.pause();
      voice.currentTime = 0;
    }
  }
}

// Compare confirmed snapshots, so rejected commands and reconnects make no move/build sound.
export function roomSound(
  previous: RoomState | null,
  room: RoomState | null,
): Sound | null {
  if (
    !previous ||
    !room ||
    previous.id !== room.id ||
    room.version <= previous.version ||
    previous.paused ||
    room.paused
  )
    return null;
  if (room.status === "FINISHED" && previous.status !== "FINISHED")
    return "finish";
  if (!previous.game && room.game) return "ready";
  if (!previous.game || !room.game)
    return room.players.length > previous.players.length ? "ready" : null;
  const before = previous.game,
    after = room.game;
  if (
    after.cells.some(
      (cell, i) =>
        cell.level !== before.cells[i].level ||
        cell.dome !== before.cells[i].dome,
    )
  )
    return "build";
  if ((after.eliminated?.length ?? 0) > (before.eliminated?.length ?? 0))
    return "ready";
  if (after.cells.some((cell, i) => cell.worker !== before.cells[i].worker))
    return before.phase === "PLACE" ? "place" : "move";
  if (
    after.selectedWorker !== null &&
    after.selectedWorker !== before.selectedWorker
  )
    return "select";
  if (after.turn !== before.turn && after.turn === room.yourSeat)
    return "ready";
  return null;
}
