import { useEffect, useState } from "react";
import { Board } from "./Board";
import { accountRequest, type Account } from "./account";
import type { RoomState } from "./types";
import { useGame } from "./useGame";
import { Settings } from "./Settings";
import { usePreferences } from "./usePreferences";
import { useSounds } from "./useSounds";

const powers: Record<string, { title: string; text: string }> = {
  Artemis: {
    title: "Second move",
    text: "You may move once more, so long as you do not return to where you started.",
  },
  Demeter: {
    title: "Second build",
    text: "You may build once more, on a different neighbouring tile.",
  },
  Triton: {
    title: "Same-level movement",
    text: "In this project’s variant, you may keep moving while every step stays at the same height.",
  },
  None: {
    title: "Classic rules",
    text: "Move once, then build once.",
  },
};
function instruction(room: RoomState) {
  if (room.paused)
    return "Match paused. Waiting for your opponent to reconnect.";
  if (!room.game)
    return room.players.length < 2
      ? "Waiting for a second player…"
      : "Choose your power, then both players ready up.";
  if (room.game.winner !== null)
    return `${room.players[room.game.winner].name} wins!`;
  if (room.game.turn !== room.yourSeat)
    return `${room.players[room.game.turn].name} is taking their turn.`;
  return (
    (
      {
        PLACE: "Place your two workers on empty tiles.",
        SELECT: "Choose a highlighted worker.",
        MOVE: "Move to a highlighted tile.",
        BUILD: "Build on a highlighted tile.",
      } as Record<string, string>
    )[room.game.phase] ?? "Your turn."
  );
}
function Rulebook() {
  return (
    <aside className="paper-panel rulebook" aria-label="How to play">
      <div>
        <span className="eyebrow">The rules</span>
        <h2>How to play</h2>
      </div>
      <div className="rulebook-columns">
        <p>
          Place two workers each. On your turn, choose one, move to an empty
          neighbouring tile, then build beside it. Neighbours include diagonals.
          Climb from level two to level three to win; a player with no legal
          move loses.
        </p>
        <p>
          Domes close a tile. This version keeps the desktop game’s limit of one
          level when climbing <em>and descending</em>. Artemis, Demeter and this
          project’s Triton variant offer optional extra actions. Use the buttons
          beside the board to finish an optional move or build.
        </p>
      </div>
    </aside>
  );
}

export default function App() {
  const game = useGame();
  const [name, setName] = useState("");
  const [password, setPassword] = useState("");
  const [authMode, setAuthMode] = useState<"guest" | "login" | "register">(
    "guest",
  );
  const [account, setAccount] = useState<Account | null>(null);
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState("");
  const [checkingAccount, setCheckingAccount] = useState(true);
  const [rules, setRules] = useState("GODS");
  const [code, setCode] = useState("");
  const [copied, setCopied] = useState(false);
  const [help, setHelp] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const settings = usePreferences();
  const sounds = useSounds(
    !checkingAccount && settings.preferences.soundEnabled,
    game.room,
    game.status,
    authError || game.error,
  );
  const room = game.room;
  const connected = game.status === "online";
  const enabled = connected && !game.busy;
  const actions =
    room?.game?.turn === room?.yourSeat && !room?.paused && enabled
      ? (room?.game?.legalActions ?? [])
      : [];
  const playerName = game.identity?.name ?? account?.name ?? name;

  useEffect(() => {
    let active = true;
    accountRequest("me")
      .then((result) => {
        if (active) {
          setAccount(result.account ?? null);
          settings.hydrate(result.account?.id ?? null, result.preferences);
        }
      })
      .catch(() => {
        /* The guest gate remains available if account discovery fails. */
      })
      .finally(() => {
        if (active) setCheckingAccount(false);
      });
    return () => {
      active = false;
    };
  }, []);

  async function enter() {
    setAuthError("");
    if (authMode === "guest") {
      game.connect(name);
      return;
    }
    setAuthBusy(true);
    try {
      const result = await accountRequest(authMode, { name, password });
      setPassword("");
      setAccount(result.account!);
      settings.hydrate(result.account!.id, result.preferences);
      game.connect(result.account!.name);
    } catch (error) {
      setAuthError((error as Error).message);
    } finally {
      setAuthBusy(false);
    }
  }
  async function signOut() {
    if (
      room?.status === "PLAYING" &&
      !confirm("Leave this match? Your opponent will win.")
    )
      return;
    setAuthBusy(true);
    setAuthError("");
    try {
      await game.disconnect();
      await settings.flush();
      if (account) await accountRequest("logout", {});
      settings.hydrate(null);
      setAccount(null);
      setName("");
      setPassword("");
      setAuthMode("guest");
    } catch (error) {
      setAuthError((error as Error).message);
    } finally {
      setAuthBusy(false);
    }
  }
  async function copyCode() {
    try {
      await navigator.clipboard.writeText(room!.id);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setAuthError(
        "The room code could not be copied. You can select it and copy it yourself.",
      );
    }
  }

  return (
    <div
      className="app parchment"
      onPointerDownCapture={() => sounds.unlock()}
      onKeyDownCapture={() => sounds.unlock()}
      onClickCapture={(event) => {
        const button = (event.target as Element).closest("button");
        if (
          button &&
          !button.disabled &&
          !button.hasAttribute("data-sound-toggle")
        )
          sounds.play(button.hasAttribute("data-rulebook") ? "book" : "click");
      }}
      onChangeCapture={(event) => {
        const target = event.target as HTMLInputElement;
        if (target.tagName === "SELECT" || target.type === "checkbox")
          sounds.play("click");
      }}
    >
      <header className="masthead">
        <a className="brand" href="/" aria-label="Santorini home">
          <img src="/assets/compass.png" alt="" />
          <span>
            Santorini<small>A game of towers and tactics</small>
          </span>
        </a>
        <nav className="header-right" aria-label="Game menu">
          <span
            className={`connection ${connected ? "live" : ""}`}
            role="status"
          >
            <i />
            {connected
              ? `${playerName} · ${game.identity?.guest ? "guest" : "account"}`
              : game.status === "offline"
                ? "Ready to play"
                : "Connecting…"}
          </span>
          <button
            className="text-button sound-toggle"
            data-sound-toggle
            disabled={checkingAccount || authBusy}
            aria-pressed={settings.preferences.soundEnabled}
            aria-label={
              settings.preferences.soundEnabled
                ? "Mute all sounds"
                : "Enable all sounds"
            }
            onClick={() => {
              const enabled = !settings.preferences.soundEnabled;
              sounds.setEnabled(enabled);
              settings.update({ soundEnabled: enabled });
              if (enabled) sounds.play("click");
            }}
          >
            <svg
              viewBox="0 0 24 24"
              width="17"
              height="17"
              aria-hidden="true"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.7"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M4 9h4l5-4v14l-5-4H4Z" />
              {settings.preferences.soundEnabled ? (
                <path d="M17 8q5 4 0 8M16 10q2 2 0 4" />
              ) : (
                <path d="m17 9 5 6m0-6-5 6" />
              )}
            </svg>
            Sound {settings.preferences.soundEnabled ? "on" : "off"}
          </button>
          <button
            className="text-button"
            aria-expanded={settingsOpen}
            aria-controls="game-settings"
            onClick={() => setSettingsOpen(!settingsOpen)}
          >
            Settings
          </button>
          <button
            className="text-button"
            data-rulebook
            onClick={() => setHelp(!help)}
            aria-expanded={help}
          >
            How to play
          </button>
          {(connected || account) && (
            <button
              className="text-button"
              disabled={authBusy}
              onClick={signOut}
            >
              {account ? "Log out" : "End guest session"}
            </button>
          )}
        </nav>
      </header>
      <main>
        {(game.error || authError) && (
          <div className="notice" role="alert">
            {authError || game.error}
          </div>
        )}
        {help && <Rulebook />}
        {settingsOpen && (
          <div id="game-settings">
            <Settings
              preferences={settings.preferences}
              onChange={settings.update}
              disabled={checkingAccount || authBusy}
              account={!!account}
              saveState={settings.saveState}
              saveError={settings.saveError}
            />
          </div>
        )}
        {!settingsOpen && settings.saveError && (
          <div className="notice" role="alert">
            {settings.saveError} Open Settings to retry.
          </div>
        )}
        {!room ? (
          <div className="landing">
            <section className="intro">
              <span className="eyebrow">A strategy game for two</span>
              <h1>
                Build the island.
                <br />
                Reach the summit.
              </h1>
              <p className="intro-copy">
                Move your workers, raise towers and block your opponent’s path.
                Climb to the third level to win. On this island, every step
                counts.
              </p>
              <div className="map-illustration">
                <img
                  className="map-palm palm-left"
                  src="/assets/palm.png"
                  alt=""
                />
                <Board illustration />
                <img
                  className="map-palm palm-right"
                  src="/assets/palm.png"
                  alt=""
                />
                <span className="map-note">
                  Five rows. Five columns. Room for a plan.
                </span>
              </div>
              <div className="field-notes">
                <span>Play online</span>
                <p>
                  Meet someone in the public queue, or share a private room code
                  with a friend. Browser and Java desktop players can sit at the
                  same table.
                </p>
              </div>
            </section>
            <section className="paper-panel lobby">
              {!connected ? (
                <>
                  <span className="eyebrow">Join the game</span>
                  <h2>
                    {account
                      ? `Welcome back, ${account.name}.`
                      : "Choose a player name"}
                  </h2>
                  {account ? (
                    <>
                      <p>
                        Your account is ready. Enter the lobby to find a match
                        or invite a friend.
                      </p>
                      <button
                        className="primary full"
                        disabled={game.status !== "offline" || authBusy}
                        onClick={() => game.connect(account.name)}
                      >
                        Return to the lobby
                      </button>
                    </>
                  ) : (
                    <>
                      <div
                        className="auth-tabs"
                        role="group"
                        aria-label="Choose how to enter"
                      >
                        {(
                          [
                            ["guest", "Guest"],
                            ["login", "Log in"],
                            ["register", "Create account"],
                          ] as const
                        ).map(([mode, label]) => (
                          <button
                            key={mode}
                            className={authMode === mode ? "active" : ""}
                            aria-pressed={authMode === mode}
                            disabled={authBusy || game.status !== "offline"}
                            onClick={() => {
                              setAuthMode(mode);
                              setAuthError("");
                              setPassword("");
                            }}
                          >
                            {label}
                          </button>
                        ))}
                      </div>
                      <p className="gate-note">
                        {authMode === "guest"
                          ? "Play without a password. Your guest name lasts for this session."
                          : authMode === "login"
                            ? "Enter your account name and password."
                            : "Reserve a name and save your settings. No email address is required."}
                      </p>
                      <form
                        onSubmit={(event) => {
                          event.preventDefault();
                          void enter();
                        }}
                      >
                        <label htmlFor="name">
                          {authMode === "guest"
                            ? "Display name"
                            : "Account name"}
                        </label>
                        <input
                          id="name"
                          maxLength={24}
                          value={name}
                          onChange={(event) => setName(event.target.value)}
                          placeholder="Your player name"
                          autoComplete={
                            authMode === "guest" ? "nickname" : "username"
                          }
                          required
                          disabled={authBusy || game.status !== "offline"}
                        />
                        {authMode !== "guest" && (
                          <>
                            <label htmlFor="password">Password</label>
                            <input
                              id="password"
                              type="password"
                              value={password}
                              onChange={(event) =>
                                setPassword(event.target.value)
                              }
                              autoComplete={
                                authMode === "login"
                                  ? "current-password"
                                  : "new-password"
                              }
                              minLength={
                                authMode === "register" ? 12 : undefined
                              }
                              maxLength={72}
                              required
                              disabled={authBusy}
                            />
                            {authMode === "register" && (
                              <p className="subtle password-note">
                                At least 12 characters. A passphrase of several
                                unrelated words is a good choice.
                              </p>
                            )}
                          </>
                        )}
                        <button
                          className="primary full"
                          disabled={
                            !name.trim() ||
                            authBusy ||
                            checkingAccount ||
                            game.status !== "offline"
                          }
                        >
                          {authBusy
                            ? "Please wait…"
                            : game.status !== "offline"
                              ? "Connecting…"
                              : authMode === "guest"
                                ? "Enter the lobby →"
                                : authMode === "login"
                                  ? "Log in and play"
                                  : "Create account and play"}
                        </button>
                      </form>
                      <p className="fine-print">
                        Names must be unique, regardless of capitalisation.
                        Guest names are reserved briefly after a disconnection
                        so you can rejoin.
                      </p>
                    </>
                  )}
                </>
              ) : (
                <>
                  <span className="eyebrow">Playing as {playerName}</span>
                  <h2>Choose a match</h2>
                  <label htmlFor="rules">Game rules</label>
                  <select
                    id="rules"
                    value={rules}
                    onChange={(event) => setRules(event.target.value)}
                  >
                    <option value="GODS">God powers</option>
                    <option value="CLASSIC">Classic · no powers</option>
                  </select>
                  <button
                    className="primary full"
                    disabled={!enabled}
                    onClick={() => game.send({ type: "QUEUE", rules })}
                  >
                    Find an opponent →
                  </button>
                  <p className="subtle">
                    Wait in the public queue for someone using these rules.
                  </p>
                  <div className="divider">
                    <span>Or invite a friend</span>
                  </div>
                  <button
                    className="secondary full"
                    disabled={!enabled}
                    onClick={() => game.send({ type: "CREATE", rules })}
                  >
                    Create a private room
                  </button>
                  <form
                    className="join"
                    onSubmit={(event) => {
                      event.preventDefault();
                      game.send({ type: "JOIN", roomId: code });
                    }}
                  >
                    <label htmlFor="code">Have a room code?</label>
                    <div>
                      <input
                        id="code"
                        maxLength={8}
                        value={code}
                        onChange={(event) =>
                          setCode(event.target.value.toUpperCase())
                        }
                        placeholder="8-character code"
                        autoComplete="off"
                      />
                      <button
                        className="secondary"
                        disabled={!enabled || code.trim().length !== 8}
                      >
                        Join
                      </button>
                    </div>
                  </form>
                </>
              )}
            </section>
          </div>
        ) : (
          <>
            <div className="room-heading">
              <div>
                <span className="eyebrow">
                  {room.publicRoom ? "Public room" : "Private room"} ·{" "}
                  {room.rules === "GODS" ? "God powers" : "Classic"}
                </span>
                <h1>
                  {room.status === "WAITING"
                    ? "Waiting for players"
                    : room.status === "FINISHED"
                      ? "Match complete"
                      : "The island awaits"}
                </h1>
              </div>
              <button
                className="text-button"
                disabled={!enabled}
                onClick={() => {
                  if (
                    room.status !== "PLAYING" ||
                    confirm("Leave this match? Your opponent will win.")
                  )
                    game.send({ type: "LEAVE" });
                }}
              >
                Leave room
              </button>
            </div>
            <div className="game-layout">
              <section className="paper-panel board-panel">
                <div className="board-top">
                  <span className="turn-dot" data-seat={room.game?.turn ?? 0} />
                  <p className="instruction" aria-live="polite">
                    {instruction(room)}
                  </p>
                </div>
                {room.paused && (
                  <p className="subtle pause-note">
                    Your opponent’s place is reserved until{" "}
                    {new Date(room.reconnectDeadline).toLocaleTimeString(
                      "en-GB",
                      { hour: "2-digit", minute: "2-digit", second: "2-digit" },
                    )}
                    . Keep this page open.
                  </p>
                )}
                <Board
                  room={room}
                  cursorColour={settings.preferences.cursorColour}
                  showLevels={settings.preferences.showLevels}
                  actions={actions}
                  onAction={(action) => game.send({ type: "ACTION", action })}
                />
                <div className="board-key">
                  <span>
                    <i className="legend-stone" />
                    Outlined tiles are available
                  </span>
                  <span>
                    {settings.preferences.showLevels
                      ? "Tower levels are shown above buildings · D means dome"
                      : "Show tower levels in Settings"}
                  </span>
                  <span>Tab, then Enter or Space, also works</span>
                </div>
              </section>
              <aside className="game-sidebar">
                <section className="paper-panel room-code">
                  <span className="eyebrow">
                    {room.publicRoom ? "Room code" : "Invite a friend"}
                  </span>
                  <div>
                    <strong>{room.id}</strong>
                    <button className="text-button" onClick={copyCode}>
                      {copied ? "Copied" : "Copy"}
                    </button>
                  </div>
                  <p className="subtle">
                    {room.publicRoom
                      ? "The public queue will find an opponent using the same rules."
                      : "Share this code with a friend to let them join."}
                  </p>
                </section>
                <section className="paper-panel players">
                  <span className="eyebrow">Players</span>
                  {room.players.map((player, seat) => (
                    <div
                      className={`player-card seat-${seat} ${seat === room.game?.turn ? "current" : ""}`}
                      key={`${seat}-${player.name}`}
                    >
                      <div className="player-marker" aria-hidden="true">
                        {seat + 1}
                      </div>
                      <div>
                        <strong>
                          {player.name}
                          {seat === room.yourSeat && <small> · you</small>}
                        </strong>
                        <p>
                          {player.god === "None" ? "No god power" : player.god}{" "}
                          ·{" "}
                          {player.connected
                            ? room.status === "WAITING"
                              ? player.ready
                                ? "ready"
                                : "not ready"
                              : "connected"
                            : "away"}
                        </p>
                      </div>
                    </div>
                  ))}
                  {room.players.length < 2 && (
                    <div className="empty-seat">
                      <span>2</span>
                      <p>Waiting for an opponent.</p>
                    </div>
                  )}
                </section>
                {room.status === "WAITING" ? (
                  <section className="paper-panel turn-panel">
                    <span className="eyebrow">Match setup</span>
                    {room.rules === "GODS" ? (
                      <>
                        <label htmlFor="god">Your god power</label>
                        <select
                          id="god"
                          value={room.players[room.yourSeat].god}
                          disabled={!enabled}
                          onChange={(event) =>
                            game.send({
                              type: "CHOOSE_GOD",
                              god: event.target.value,
                            })
                          }
                        >
                          {Object.keys(powers)
                            .filter((god) => god !== "None")
                            .map((god) => (
                              <option
                                key={god}
                                disabled={room.players.some(
                                  (player, seat) =>
                                    seat !== room.yourSeat &&
                                    player.god === god,
                                )}
                              >
                                {god}
                              </option>
                            ))}
                        </select>
                        <p>{powers[room.players[room.yourSeat].god]?.text}</p>
                      </>
                    ) : (
                      <p>
                        Classic rules: choose a worker, move once, then build
                        once.
                      </p>
                    )}
                    <button
                      className="primary full"
                      disabled={!enabled}
                      onClick={() => game.send({ type: "READY" })}
                    >
                      {room.players[room.yourSeat].ready
                        ? "Not ready yet"
                        : "Ready to play"}
                    </button>
                  </section>
                ) : (
                  <section className="paper-panel turn-panel">
                    <span className="eyebrow">
                      {room.status === "FINISHED" ? "Result" : "Your power"}
                    </span>
                    <h3>
                      {room.status === "FINISHED"
                        ? room.game?.winner === room.yourSeat
                          ? "You win"
                          : "Your opponent wins"
                        : powers[room.players[room.yourSeat].god]?.title}
                    </h3>
                    <p>
                      {room.status === "FINISHED"
                        ? `${instruction(room)} ${room.result ? `${room.result}.` : ""}`
                        : powers[room.players[room.yourSeat].god]?.text}
                    </p>
                    <div className="extra-actions">
                      {actions
                        .filter(
                          (action) =>
                            action.row === null && action.type !== "SELECT",
                        )
                        .map((action) => (
                          <button
                            className="secondary full"
                            key={action.type}
                            onClick={() =>
                              game.send({ type: "ACTION", action })
                            }
                          >
                            {
                              (
                                {
                                  CANCEL: "Choose another worker",
                                  END_MOVE: "Continue to build",
                                  END_TURN: "End turn",
                                } as Record<string, string>
                              )[action.type]
                            }
                          </button>
                        ))}
                    </div>
                    {room.status === "FINISHED" ? (
                      <button
                        className="primary full"
                        disabled={!enabled}
                        onClick={() => game.send({ type: "LEAVE" })}
                      >
                        Back to lobby
                      </button>
                    ) : (
                      <button
                        className="text-button resign"
                        disabled={!enabled}
                        onClick={() => {
                          if (
                            confirm(
                              "Resign this match? Your opponent will win.",
                            )
                          )
                            game.send({ type: "RESIGN" });
                        }}
                      >
                        Concede this match
                      </button>
                    )}
                  </section>
                )}
              </aside>
            </div>
          </>
        )}
      </main>
      <footer className="footer">
        <span>Santorini · an island for two</span>
        <span>
          Designed and Developed by{" "}
          <a
            href="https://github.com/Vael2196"
            target="_blank"
            rel="noreferrer"
          >
            Vael (Vadim Filyakin)
          </a>
        </span>
        <span>Move. Build. Climb.</span>
      </footer>
    </div>
  );
}
