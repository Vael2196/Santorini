import { useEffect, useState } from "react";
import { Board } from "./Board";
import { accountRequest, type Account } from "./account";
import type { RoomState } from "./types";
import { useGame } from "./useGame";

const powers: Record<string, { title: string; text: string }> = {
  Artemis: {
    title: "The nimble one",
    text: "You may move once more, so long as you do not return to where you started.",
  },
  Demeter: {
    title: "The industrious one",
    text: "You may build once more, on a different neighbouring tile.",
  },
  Triton: {
    title: "The wanderer",
    text: "In this project’s variant, you may keep moving while every step stays at the same height.",
  },
  None: {
    title: "A quiet day",
    text: "One move, one build. Plenty to think about.",
  },
};
function instruction(room: RoomState) {
  if (room.paused)
    return "A brief intermission. Waiting for your opponent to return.";
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
        <span className="eyebrow">A page from the rulebook</span>
        <h2>A modest guide to island life</h2>
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
          beside the board when you have done enough.
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
        if (active) setAccount(result.account ?? null);
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
      !confirm("Leave the island? Your opponent will win this match.")
    )
      return;
    setAuthBusy(true);
    setAuthError("");
    try {
      await game.disconnect();
      if (account) await accountRequest("logout", {});
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
    <div className="app parchment">
      <header className="masthead">
        <a className="brand" href="/" aria-label="Santorini home">
          <img src="/assets/compass.png" alt="" />
          <span>
            Santorini<small>A little island on your table</small>
          </span>
        </a>
        <nav className="header-right" aria-label="Island menu">
          <span
            className={`connection ${connected ? "live" : ""}`}
            role="status"
          >
            <i />
            {connected
              ? `${playerName} · ${game.identity?.guest ? "guest" : "account"}`
              : game.status === "offline"
                ? "The island is open"
                : "Finding our way back…"}
          </span>
          <button
            className="text-button"
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
        {!room ? (
          <div className="landing">
            <section className="intro">
              <span className="eyebrow">The island ledger · two players</span>
              <h1>
                A few stones.
                <br />A little mischief.
              </h1>
              <p className="intro-copy">
                Two builders arrive on a small island. They put up towers, get
                in one another’s way, and try to reach the top. Shall we see how
                it goes?
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
                  Not to scale. Ambition rarely is.
                </span>
              </div>
              <div className="field-notes">
                <span>01 / Choose your company</span>
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
                  <span className="eyebrow">The guest book</span>
                  <h2>
                    {account
                      ? `Welcome back, ${account.name}.`
                      : "What shall we call you?"}
                  </h2>
                  {account ? (
                    <>
                      <p>
                        Your name is still here. Your last cup of tea, alas, is
                        not.
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
                          ? "No password, no paperwork. Your name is yours for this visit."
                          : authMode === "login"
                            ? "A familiar name and its password will do."
                            : "Keep a name of your own for future visits. No email address needed."}
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
                          placeholder="A name for the guest book"
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
                                At least 12 characters. A handful of unrelated
                                words works nicely.
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
                            ? "A moment, please…"
                            : game.status !== "offline"
                              ? "Connecting…"
                              : authMode === "guest"
                                ? "Enter the lobby →"
                                : authMode === "login"
                                  ? "Log in and take a seat"
                                  : "Create account and take a seat"}
                        </button>
                      </form>
                      <p className="fine-print">
                        Names are shared by guests and accounts. Changing the
                        capital letters will not borrow someone else’s name.
                        Guest names are kept briefly after a disconnection so
                        you can return.
                      </p>
                    </>
                  )}
                </>
              ) : (
                <>
                  <span className="eyebrow">A seat for {playerName}</span>
                  <h2>Who’s joining you?</h2>
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
                  {room.publicRoom ? "Public table" : "Private table"} ·{" "}
                  {room.rules === "GODS" ? "God powers" : "Classic"}
                </span>
                <h1>
                  {room.status === "WAITING"
                    ? "Pull up a chair."
                    : room.status === "FINISHED"
                      ? "The dust settles."
                      : "A small matter of strategy."}
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
                    The seat is reserved until{" "}
                    {new Date(room.reconnectDeadline).toLocaleTimeString(
                      "en-GB",
                      { hour: "2-digit", minute: "2-digit", second: "2-digit" },
                    )}
                    . Keep this page open.
                  </p>
                )}
                <Board
                  room={room}
                  actions={actions}
                  onAction={(action) => game.send({ type: "ACTION", action })}
                />
                <div className="board-key">
                  <span>
                    <i className="legend-stone" />
                    Outlined tiles are available
                  </span>
                  <span>Numbers show tower height · D means dome</span>
                  <span>Tab, then Enter or Space, also works</span>
                </div>
              </section>
              <aside className="game-sidebar">
                <section className="paper-panel room-code">
                  <span className="eyebrow">
                    {room.publicRoom
                      ? "Table reference"
                      : "Pass this to your friend"}
                  </span>
                  <div>
                    <strong>{room.id}</strong>
                    <button className="text-button" onClick={copyCode}>
                      {copied ? "Copied" : "Copy"}
                    </button>
                  </div>
                  <p className="subtle">
                    {room.publicRoom
                      ? "The public queue supplies the other seat."
                      : "Only someone with this code can join this table."}
                  </p>
                </section>
                <section className="paper-panel players">
                  <span className="eyebrow">The company</span>
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
                              : "at the table"
                            : "away"}
                        </p>
                      </div>
                    </div>
                  ))}
                  {room.players.length < 2 && (
                    <div className="empty-seat">
                      <span>2</span>
                      <p>One chair still waiting.</p>
                    </div>
                  )}
                </section>
                {room.status === "WAITING" ? (
                  <section className="paper-panel turn-panel">
                    <span className="eyebrow">Before we begin</span>
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
                      {room.status === "FINISHED"
                        ? "The last page"
                        : "Your turn notes"}
                    </span>
                    <h3>
                      {room.status === "FINISHED"
                        ? room.game?.winner === room.yourSeat
                          ? "Nicely built."
                          : "Another day, another tower."
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
          <a href="https://github.com/Vael2196" target="_blank" rel="noreferrer">
            Vael (Vadim Filyakin)
          </a>
        </span>
        <span>Mind the domes.</span>
      </footer>
    </div>
  );
}
