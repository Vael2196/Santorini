import { expect, test, vi } from "vitest";
import { roomSound, SoundBank } from "../src/sounds";
import { emptyCells } from "../src/Board";
import type { RoomState } from "../src/types";

test("sound waits for an interaction, mute stops active clips, and playback failure stays harmless", async () => {
  const voice = {
    play: vi.fn().mockRejectedValue(new Error("Playback blocked")),
    pause: vi.fn(),
    currentTime: 0,
    volume: 0,
  };
  const create = vi.fn(() => voice as unknown as HTMLAudioElement);
  const bank = new SoundBank(create);
  bank.play("build");
  expect(create).not.toHaveBeenCalled();
  bank.unlock();
  bank.play("build");
  await Promise.resolve();
  expect(voice.play).toHaveBeenCalledOnce();
  bank.setEnabled(false);
  expect(voice.pause).toHaveBeenCalledOnce();
  expect(voice.currentTime).toBe(0);
  for (const sound of ["click", "move", "finish", "error"] as const)
    bank.play(sound);
  expect(voice.play).toHaveBeenCalledOnce();
  bank.setEnabled(true);
  bank.play("build");
  await Promise.resolve();
  expect(voice.play).toHaveBeenCalledTimes(2);
});

test("confirmed building changes sound once while initial, stale and reconnect snapshots stay silent", () => {
  const before: RoomState = {
    id: "ABCDEFGH",
    publicRoom: false,
    rules: "CLASSIC",
    status: "PLAYING",
    version: 5,
    players: [
      { name: "One", god: "None", ready: true, connected: true },
      { name: "Two", god: "None", ready: true, connected: true },
    ],
    yourSeat: 0,
    paused: false,
    reconnectDeadline: 0,
    result: null,
    game: {
      phase: "BUILD",
      turn: 0,
      selectedWorker: 0,
      winner: null,
      cells: emptyCells(),
      legalActions: [],
    },
  };
  const after = structuredClone(before);
  after.version++;
  after.game!.cells[0].level = 1;
  expect(roomSound(null, after)).toBeNull();
  expect(roomSound(before, after)).toBe("build");
  expect(roomSound(after, after)).toBeNull();
  expect(roomSound(after, before)).toBeNull();
  expect(roomSound({ ...before, paused: true }, after)).toBeNull();
  expect(roomSound(before, { ...after, paused: true })).toBeNull();
  expect(roomSound(before, { ...after, id: "OTHER123" })).toBeNull();
});
