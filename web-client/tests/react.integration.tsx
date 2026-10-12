import { StrictMode } from "react";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  cleanup,
} from "@testing-library/react";
import { afterEach, expect, test } from "vitest";
import WebSocket from "ws";
import App from "../src/App";
import type { RoomState, ServerEvent } from "../src/types";
import { playedSounds } from "./setup";
import { accountRequest } from "../src/account";

const peers: Guest[] = [];
class Guest {
  socket: WebSocket;
  room: RoomState | null = null;
  token: string | null = null;
  events: ServerEvent[] = [];
  constructor() {
    this.socket = new WebSocket("ws://127.0.0.1:8080/ws", {
      origin: "http://localhost:5173",
    });
    this.socket.on("message", (data) => {
      const event: ServerEvent = JSON.parse(data.toString());
      this.events.push(event);
      if (event.type === "WELCOME") this.token = event.token;
      if (event.type === "STATE") this.room = event.room;
      if (event.type === "LEFT") this.room = null;
    });
    peers.push(this);
  }
  async connect(name = "Remote-" + crypto.randomUUID().slice(0, 8)) {
    await waitFor(() => expect(this.socket.readyState).toBe(WebSocket.OPEN));
    this.socket.send(JSON.stringify({ type: "HELLO", name }));
    await waitFor(() => expect(this.token).toBeTruthy());
  }
  send(type: string, fields: Record<string, unknown> = {}) {
    this.socket.send(
      JSON.stringify({ type, version: this.room?.version, ...fields }),
    );
  }
}
afterEach(() => {
  cleanup();
  for (const peer of peers.splice(0)) peer.socket.close();
  sessionStorage.clear();
});

let currentHost = "";
async function lobby() {
  currentHost = "React-" + crypto.randomUUID().slice(0, 8);
  render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
  fireEvent.change(screen.getByLabelText("Display name"), {
    target: { value: currentHost },
  });
  const enter = screen.getByRole("button", { name: "Enter the lobby →" });
  await waitFor(() => expect(enter).toBeEnabled());
  fireEvent.click(enter);
  await screen.findByRole("button", { name: "Create a private room" });
}
function roomCode() {
  return document.querySelector(".room-code strong")!.textContent!;
}
async function clickCell(row: number, col: number) {
  const cell = screen.getByTestId(`cell-${row}-${col}`);
  await waitFor(() => expect(cell).toHaveAttribute("aria-disabled", "false"));
  fireEvent.click(cell);
}

test("React and another client play a private match and recover after remount", async () => {
  await lobby();
  fireEvent.pointerDown(
    screen.getByRole("button", { name: "Settings", exact: true }),
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Settings", exact: true }),
  );
  fireEvent.click(screen.getByLabelText("Show tower levels"));
  fireEvent.change(screen.getByLabelText("Game rules"), {
    target: { value: "CLASSIC" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Create a private room" }),
  );
  await waitFor(() =>
    expect(document.querySelector(".room-code strong")).not.toBeNull(),
  );
  const code = roomCode();
  const guest = new Guest();
  await guest.connect();
  guest.send("JOIN", { roomId: code });
  await waitFor(() => expect(guest.room?.players).toHaveLength(2));
  await waitFor(() =>
    expect(document.querySelectorAll(".player-card")).toHaveLength(2),
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Ready to play", exact: true }),
  );
  await waitFor(() => expect(guest.room?.players[0].ready).toBe(true));
  guest.send("READY");
  await waitFor(() => expect(guest.room?.status).toBe("PLAYING"));
  await waitFor(() =>
    expect(document.querySelector(".instruction")).toHaveTextContent(
      "Place your two workers",
    ),
  );
  await clickCell(0, 0);
  await clickCell(0, 1);
  await waitFor(() => expect(guest.room?.game?.turn).toBe(1));
  guest.send("ACTION", {
    action: { type: "PLACE", worker: null, row: 4, col: 4 },
  });
  await waitFor(() =>
    expect(guest.room?.game?.cells[24].worker).not.toBeNull(),
  );
  guest.send("ACTION", {
    action: { type: "PLACE", worker: null, row: 4, col: 3 },
  });
  await waitFor(() =>
    expect(document.querySelector(".instruction")).toHaveTextContent(
      "Choose a highlighted worker",
    ),
  );
  await clickCell(0, 0);
  expect(screen.getByTestId("cell-4-4")).toHaveAttribute(
    "aria-disabled",
    "true",
  );
  await clickCell(1, 0);
  const buildTile = screen.getByTestId("cell-0-0");
  await waitFor(() =>
    expect(buildTile).toHaveAttribute("aria-disabled", "false"),
  );
  expect(buildTile.parentElement).toHaveClass("cursor-build");
  fireEvent.keyDown(buildTile, { key: "Enter" });
  await waitFor(() =>
    expect(screen.getByTestId("cell-0-0")).toHaveAttribute(
      "aria-label",
      "A1, level 1",
    ),
  );
  expect(document.querySelector(".level-marker")).toHaveTextContent("1");
  expect(playedSounds.some((src) => src.endsWith("/build.mp3"))).toBe(true);
  cleanup();
  render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
  await waitFor(() =>
    expect(document.querySelector(".room-code strong")).toHaveTextContent(code),
  );
  await waitFor(() => expect(guest.room?.paused).toBe(false));
  guest.send("RESIGN");
  await waitFor(() =>
    expect(document.querySelector(".instruction")).toHaveTextContent(
      `${currentHost} wins`,
    ),
  );
  fireEvent.click(screen.getByRole("button", { name: "Back to lobby" }));
  await screen.findByRole("button", { name: "Find an opponent →" });
});

test("public matchmaking excludes a React private room and queue cancellation works", async () => {
  await lobby();
  fireEvent.click(
    screen.getByRole("button", { name: "Create a private room" }),
  );
  await waitFor(() =>
    expect(document.querySelector(".room-code strong")).not.toBeNull(),
  );
  const privateCode = roomCode();
  const guest = new Guest();
  await guest.connect();
  guest.send("QUEUE", { rules: "GODS" });
  await waitFor(() => expect(guest.room).not.toBeNull());
  expect(guest.room!.id).not.toBe(privateCode);
  expect(guest.room!.players).toHaveLength(1);
  fireEvent.click(screen.getByRole("button", { name: "Leave room" }));
  await screen.findByRole("button", { name: "Find an opponent →" });
  fireEvent.click(screen.getByRole("button", { name: "Find an opponent →" }));
  await waitFor(() =>
    expect(document.querySelectorAll(".player-card")).toHaveLength(2),
  );
  expect(roomCode()).toBe(guest.room!.id);
  fireEvent.click(screen.getByRole("button", { name: "Leave room" }));
  await screen.findByRole("button", { name: "Find an opponent →" });
});

test("invalid private room codes show an error without discarding the guest session", async () => {
  await lobby();
  fireEvent.change(screen.getByLabelText("Have a room code?"), {
    target: { value: "WRONG123" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Join", exact: true }));
  await waitFor(() =>
    expect(screen.getByRole("alert")).toHaveTextContent("No room found"),
  );
  expect(
    screen.getByRole("button", { name: "Create a private room" }),
  ).toBeEnabled();
});

test("accounts can register, survive a remount, log out and log in again", async () => {
  const accountName = "Ledger-" + crypto.randomUUID().slice(0, 8);
  render(<App />);
  fireEvent.click(
    screen.getByRole("button", { name: "Create account", exact: true }),
  );
  fireEvent.change(screen.getByLabelText("Account name"), {
    target: { value: accountName },
  });
  fireEvent.change(screen.getByLabelText("Password"), {
    target: { value: "a rather sturdy password" },
  });
  const submit = screen.getByRole("button", {
    name: "Create account and play",
  });
  await waitFor(() => expect(submit).toBeEnabled());
  fireEvent.click(submit);
  await screen.findByRole("button", { name: "Create a private room" });
  expect(screen.getByRole("status")).toHaveTextContent(
    `${accountName} · account`,
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Settings", exact: true }),
  );
  fireEvent.change(screen.getByLabelText("In-game cursor colour"), {
    target: { value: "teal" },
  });
  fireEvent.click(screen.getByLabelText("Show tower levels"));
  fireEvent.click(screen.getByRole("button", { name: "Sound volume" }));
  fireEvent.change(screen.getByRole("slider"), { target: { value: "35" } });
  fireEvent.click(screen.getByRole("button", { name: "Increase volume" }));
  expect(screen.getByRole("slider")).toHaveValue("40");
  fireEvent.keyDown(document, { key: "Escape" });
  fireEvent.click(screen.getByRole("button", { name: "Mute all sounds" }));
  await screen.findByText("Saved to your account");
  // Remove device settings: returning choices must come from the account store.
  localStorage.clear();
  cleanup();
  render(<App />);
  await screen.findByRole("button", { name: "Create a private room" });
  fireEvent.click(
    screen.getByRole("button", { name: "Settings", exact: true }),
  );
  await waitFor(() =>
    expect(screen.getByLabelText("In-game cursor colour")).toHaveValue("teal"),
  );
  expect(screen.getByLabelText("Show tower levels")).toBeChecked();
  fireEvent.click(screen.getByRole("button", { name: "Sound volume" }));
  expect(screen.getByRole("slider")).toHaveValue("40");
  fireEvent.keyDown(document, { key: "Escape" });
  expect(
    screen.getByRole("button", { name: "Enable all sounds" }),
  ).toBeVisible();
  await waitFor(() =>
    expect(screen.getByRole("button", { name: "Log out" })).toBeEnabled(),
  );
  fireEvent.click(screen.getByRole("button", { name: "Log out" }));
  await screen.findByRole("button", { name: "Create account", exact: true });
  fireEvent.click(screen.getByRole("button", { name: "Log in", exact: true }));
  fireEvent.change(screen.getByLabelText("Account name"), {
    target: { value: accountName.toLowerCase() },
  });
  fireEvent.change(screen.getByLabelText("Password"), {
    target: { value: "a rather sturdy password" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Log in and play" }));
  await screen.findByRole("button", { name: "Create a private room" });
  expect(screen.getByRole("status")).toHaveTextContent(accountName);
  expect(screen.getByLabelText("In-game cursor colour")).toHaveValue("teal");
  expect(screen.getByLabelText("Show tower levels")).toBeChecked();
  fireEvent.click(screen.getByRole("button", { name: "Sound volume" }));
  expect(screen.getByRole("slider")).toHaveValue("40");
  fireEvent.keyDown(document, { key: "Escape" });
  expect(
    screen.getByRole("button", { name: "Enable all sounds" }),
  ).toBeVisible();
});

test("guest settings survive a remount and one button silences all interaction sounds", async () => {
  await lobby();
  fireEvent.pointerDown(
    screen.getByRole("button", { name: "Settings", exact: true }),
  );
  fireEvent.click(
    screen.getByRole("button", { name: "Settings", exact: true }),
  );
  expect(playedSounds.length).toBeGreaterThan(0);
  expect(screen.getByLabelText("In-game cursor colour")).toHaveValue("brown");
  fireEvent.change(screen.getByLabelText("In-game cursor colour"), {
    target: { value: "ivory" },
  });
  fireEvent.click(screen.getByLabelText("Show tower levels"));
  fireEvent.click(screen.getByRole("button", { name: "Mute all sounds" }));
  playedSounds.length = 0;
  fireEvent.click(screen.getByRole("button", { name: "How to play" }));
  fireEvent.click(
    screen.getByRole("button", { name: "Create a private room" }),
  );
  await waitFor(() =>
    expect(document.querySelector(".room-code strong")).not.toBeNull(),
  );
  expect(
    document.querySelector(".board-panel .isometric-board"),
  ).toHaveAttribute("data-cursor-colour", "ivory");
  expect(playedSounds).toHaveLength(0);
  cleanup();
  render(<App />);
  await screen.findByRole("button", { name: "Leave room" });
  fireEvent.click(
    screen.getByRole("button", { name: "Settings", exact: true }),
  );
  expect(screen.getByLabelText("In-game cursor colour")).toHaveValue("ivory");
  expect(screen.getByLabelText("Show tower levels")).toBeChecked();
  expect(
    screen.getByRole("button", { name: "Enable all sounds" }),
  ).toBeVisible();
  expect(playedSounds).toHaveLength(0);
});

test("an unsuccessful account save is visible and retry saves the current choices", async () => {
  await accountRequest("register", {
    name: "Settings-" + crypto.randomUUID().slice(0, 8),
    password: "a rather sturdy password",
  });
  render(<App />);
  await screen.findByRole("button", { name: "Return to the lobby" });
  fireEvent.click(
    screen.getByRole("button", { name: "Settings", exact: true }),
  );
  await waitFor(() =>
    expect(screen.getByLabelText("In-game cursor colour")).toBeEnabled(),
  );
  const transport = globalThis.fetch;
  globalThis.fetch = async (input, init) =>
    String(input).endsWith("/api/auth/preferences")
      ? new Response(JSON.stringify({ message: "Temporarily unavailable" }), {
          status: 503,
          headers: { "Content-Type": "application/json" },
        })
      : transport(input, init);
  try {
    fireEvent.change(screen.getByLabelText("In-game cursor colour"), {
      target: { value: "charcoal" },
    });
    await screen.findByRole("button", { name: "Retry saving" });
    expect((await accountRequest("me")).preferences?.cursorColour).toBe(
      "brown",
    );
  } finally {
    globalThis.fetch = transport;
  }
  fireEvent.click(screen.getByRole("button", { name: "Retry saving" }));
  await screen.findByText("Saved to your account");
  expect((await accountRequest("me")).preferences?.cursorColour).toBe(
    "charcoal",
  );
});

test("duplicate guest names are rejected and explicitly ending a visit releases the name", async () => {
  await lobby();
  const duplicate = new WebSocket("ws://127.0.0.1:8080/ws", {
    origin: "http://localhost:5173",
  });
  const events: ServerEvent[] = [];
  duplicate.on("message", (data) => events.push(JSON.parse(data.toString())));
  try {
    await waitFor(() => expect(duplicate.readyState).toBe(WebSocket.OPEN));
    duplicate.send(
      JSON.stringify({ type: "HELLO", name: currentHost.toUpperCase() }),
    );
    await waitFor(() =>
      expect(events.find((event) => event.code === "NAME_TAKEN")).toBeTruthy(),
    );
    fireEvent.click(screen.getByRole("button", { name: "End guest session" }));
    await screen.findByRole("button", { name: "Enter the lobby →" });
    duplicate.send(JSON.stringify({ type: "HELLO", name: currentHost }));
    await waitFor(() =>
      expect(events.find((event) => event.type === "WELCOME")).toBeTruthy(),
    );
  } finally {
    duplicate.close();
  }
});

test("computer practice places and takes a turn without another client", async () => {
  await lobby();
  fireEvent.change(screen.getByLabelText("Game rules"), {
    target: { value: "CLASSIC" },
  });
  fireEvent.click(
    screen.getByRole("button", { name: "Play against the computer" }),
  );
  await screen.findByText("Computer (AI)");
  fireEvent.click(
    screen.getByRole("button", { name: "Ready to play", exact: true }),
  );
  await clickCell(0, 0);
  await clickCell(0, 1);
  await waitFor(
    () =>
      expect(document.querySelector(".instruction")).toHaveTextContent(
        "Choose a highlighted worker",
      ),
    { timeout: 5000 },
  );
  expect(document.querySelectorAll(".piece-1")).toHaveLength(2);
  await clickCell(0, 0);
  await clickCell(1, 0);
  await clickCell(0, 0);
  await waitFor(
    () =>
      expect(document.querySelector(".instruction")).toHaveTextContent(
        "Choose a highlighted worker",
      ),
    { timeout: 5000 },
  );
  expect(
    screen.queryByRole("region", { name: "Room chat" }),
  ).not.toBeInTheDocument();
});

for (const [mode, count, workers] of [
  ["THREE", 3, 2],
  ["FOUR_FFA", 4, 1],
  ["TEAMS", 4, 1],
] as const) {
  test(`${mode} fills all seats and renders the correct worker ownership`, async () => {
    await lobby();
    fireEvent.change(screen.getByLabelText("Players and teams"), {
      target: { value: mode },
    });
    if (mode === "THREE")
      fireEvent.change(screen.getByLabelText("Game rules"), {
        target: { value: "CLASSIC" },
      });
    fireEvent.click(
      screen.getByRole("button", { name: "Create a private room" }),
    );
    await waitFor(() =>
      expect(document.querySelector(".room-code strong")).not.toBeNull(),
    );
    const guests: Guest[] = [];
    for (let i = 1; i < count; i++) {
      const p = new Guest();
      await p.connect();
      p.send("JOIN", { roomId: roomCode() });
      await waitFor(() => expect(p.room?.players).toHaveLength(i + 1));
      guests.push(p);
    }
    fireEvent.click(
      screen.getByRole("button", { name: "Ready to play", exact: true }),
    );
    for (const p of guests) {
      await waitFor(() => expect(p.room?.players[0].ready).toBe(true));
      p.send("READY");
      await waitFor(() =>
        expect(p.room?.players[p.room.yourSeat].ready).toBe(true),
      );
    }
    await waitFor(() => expect(guests[0].room?.status).toBe("PLAYING"));
    for (let col = 0; col < workers; col++) await clickCell(0, col);
    for (let seat = 1; seat < count; seat++)
      for (let col = 0; col < workers; col++) {
        const p = guests[seat - 1];
        await waitFor(() => expect(p.room?.game?.turn).toBe(seat));
        p.send("ACTION", {
          action: { type: "PLACE", worker: null, row: seat, col },
        });
        await waitFor(() =>
          expect(p.room?.game?.cells[seat * 5 + col].owner).toBe(seat),
        );
      }
    await waitFor(() =>
      expect(document.querySelectorAll(".piece")).toHaveLength(count * workers),
    );
    expect(document.querySelectorAll(".piece-2")).toHaveLength(workers);
    guests[0].send("RESIGN");
    if (mode === "TEAMS")
      await waitFor(() =>
        expect(document.querySelector(".instruction")).toHaveTextContent(
          " and ",
        ),
      );
    else {
      await waitFor(() =>
        expect(guests[0].room?.game?.eliminated).toContain(1),
      );
      expect(guests[0].room?.status).toBe("PLAYING");
    }
  });
}

test("room chat renders plain text, rejects links and restores history", async () => {
  await lobby();
  fireEvent.click(
    screen.getByRole("button", { name: "Create a private room" }),
  );
  await waitFor(() =>
    expect(document.querySelector(".room-code strong")).not.toBeNull(),
  );
  const p = new Guest();
  await p.connect();
  p.send("JOIN", { roomId: roomCode() });
  await waitFor(() => expect(p.room?.players).toHaveLength(2));
  fireEvent.change(screen.getByLabelText("Message"), {
    target: { value: "https://example.org" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Send message" }));
  await screen.findByText("Links and email addresses are not allowed in chat");
  fireEvent.change(screen.getByLabelText("Message"), {
    target: { value: "<img src=x> hello" },
  });
  fireEvent.click(screen.getByRole("button", { name: "Send message" }));
  await waitFor(() =>
    expect(p.events.some((e) => e.chat?.text === "<img src=x> hello")).toBe(
      true,
    ),
  );
  expect(screen.getByRole("log")).toHaveTextContent("<img src=x> hello");
  expect(screen.getByRole("log").querySelector("img")).toBeNull();
  expect(
    screen.getByRole("button", { name: "Ready to play", exact: true }),
  ).toBeEnabled();
  cleanup();
  render(<App />);
  await waitFor(() =>
    expect(screen.getByRole("log")).toHaveTextContent("<img src=x> hello"),
  );
});
