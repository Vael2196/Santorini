# Online architecture notes

## Boundaries

The game core compiles the existing model and god strategies without Swing. The desktop module
excludes those sources and depends on the core JAR, preserving existing class names and local save
formats. The network game never opens a Swing dialogue or terminates the application process.

The server owns every online board. A command identifies an intention; the connection identifies
the player. The server checks room membership, turn ownership, the snapshot version and the legal
action before applying it. The immutable snapshot is sent separately to each player with their seat.

Membership changes take the lobby lock followed by the room lock. Game actions take only their
room's lock. Name reservations have their own lock, shared with account registration. SQL has a
unique canonical-name constraint as a further safeguard. These boundaries allow independent rooms
to progress while serialising admissions and duplicate-name checks.

## Names and accounts

Names are normalised with Unicode NFKC, surrounding whitespace is removed, repeated spaces are
collapsed and matching is case-insensitive. Names contain 1–24 letters, numbers, spaces, hyphens,
apostrophes or underscores and begin with a letter or number. Registered and guest names occupy
one namespace. Registering a name used by an active guest is rejected; the guest must end the visit first.

Guest names are memory reservations. `GOODBYE` releases them immediately. A normal disconnection
keeps the identity and seat through the configured reconnect window (60 seconds by default), then
releases the name. Match history displayed to an opponent can retain the old label after release;
that label is no longer an identity reservation.

Accounts use UUID identities and persist in SQL. Account names remain reserved after logout and
server restart. `player_accounts` holds the identity, canonical-name digest, display name, BCrypt
hash and creation time. The digest avoids depending on a database's text collation for name matching.
`account_sessions` holds SHA-256 digests of cryptographically random session secrets and expiry times.
The migration is shared by H2 and MySQL; Flyway applies it at startup.

The browser authenticates through a revocable HttpOnly cookie with SameSite=Lax and a configured
expiry (seven days by default). Public HTTPS deployments enable its Secure flag. CSRF tokens protect
registration, login, logout and preference updates. Passwords are 12 characters or longer and at most 72 UTF-8 bytes,
matching BCrypt's input limit. Authentication endpoints limit attempts and concurrent password hashing.
The desktop uses the same endpoints with cookies in memory and does not save passwords.

WebSocket handshakes resolve account cookies on the server. `HELLO.name` cannot change an authenticated
account's name. Resuming an account's game token also requires the authenticated account identity.
A new socket cannot claim an account by submitting its name or UUID. Account session revocation or
expiry closes its socket. One active game connection per account prevents a second window from taking
over a match; reconnecting with that match's secret token replaces the interrupted connection.

## HTTP account endpoints

| Endpoint | Behaviour |
| --- | --- |
| `GET /api/auth/csrf` | Issues the CSRF cookie and returns the request token |
| `GET /api/auth/me` | Returns the authenticated account and preferences, or an unauthenticated result |
| `POST /api/auth/register` | JSON name/password; creates an account and sets its session cookie |
| `POST /api/auth/login` | JSON name/password; verifies the hash and sets a fresh session cookie |
| `POST /api/auth/preferences` | Saves cursor colour, tower-level display and sound settings for the cookie’s account |
| `POST /api/auth/logout` | Revokes the session, closes the account's game seat and clears the cookie |

State-changing requests include `X-XSRF-TOKEN`. Responses carrying identity information are not cached.
There is no email collection, password-recovery service, leaderboard or points ledger in this version.
Stable account IDs and server-side identities are the foundation for those future changes.

Account preferences live in `player_accounts`; migration V2 supplies defaults for existing accounts.
`cursorColour` accepts `brown`, `charcoal`, `teal` or `ivory`; `showLevels` and `soundEnabled` are
required booleans. Preference updates resolve the account exclusively from its authenticated cookie.
The browser serialises saves and reports failures with a retry control. Logout waits for pending
saves. Guest preferences use localStorage and have no server-side account record.

## Game protocol

`/ws` accepts JSON text messages. Coordinates use `row` and `col`, each 0–4. Worker IDs 0–1 belong
to seat zero and 2–3 to seat one. Public room snapshots contain names and game state, never secrets.

| Command | Fields and behaviour |
| --- | --- |
| `HELLO` | Guest `name`, or secret `token` for recovery; authenticated account identity comes from the handshake |
| `GOODBYE` | Ends the visit, forfeits an active match and releases the game session |
| `CREATE` | `rules`: `CLASSIC` or `GODS`; makes a private waiting room |
| `QUEUE` | Rules; joins the oldest compatible public waiting room or creates one |
| `JOIN` | `roomId`; joins a waiting room with an available seat |
| `CHOOSE_GOD` | God and room version; chooses a unique card and clears readiness |
| `READY` | Version; toggles readiness and starts when both connected players are ready |
| `ACTION` | Version, request ID and an action from the current legal-action list |
| `RESIGN` | Version; concedes the match |
| `LEAVE` | Cancels a waiting seat or forfeits an active match, retaining the player session |
| `PING` | Keeps the connection active and receives `PONG` |

Actions are `PLACE`, `SELECT`, `CANCEL`, `MOVE`, `END_MOVE`, `BUILD` and `END_TURN`. Unused worker
or coordinate fields are null. Versions prevent stale or duplicate mutations. Clients display errors
and fresh snapshots; they do not automatically replay actions.

`WELCOME` gives the client its private match token and its own identity (name, guest flag and account
ID where applicable). `STATE` carries the room, `LEFT` returns to the lobby, `BYE` ends the visit,
`ERROR` supplies a stable code and readable message, and `PONG` acknowledges the heartbeat.

Browser match tokens use sessionStorage, separate from the account cookie. Desktop guest recovery
tokens use Java preferences keyed by server and name. A room code admits a new seat; it cannot resume
another player's identity. Invalid tokens require a new session rather than a guessed identity.

## Interface and assets

The browser renders a depth-sorted 5×5 SVG board. Original isometric PNG tiles are positioned on the
SVG; towers, domes and workers use lightweight vector shapes. Transparent diamond hit areas follow
the logical cells. Only legal actions are clickable and keyboard-focusable. Tool cursors apply to those
hit areas, so a hammer is never offered as a promise of an illegal build.

Parchment textures and framed panels use CSS. Narrow screens put the board above the player notes;
SVG scales without changing coordinates or game rules. Motion honours reduced-motion preferences.
Assets are served locally, with no third-party graphics, font or WebGL runtime. CC0 licence texts are
kept beside the selected assets. The original archives are not copied into the repository.

The board uses a dark perimeter and one diamond outline per tile. Keyboard focus and worker selection
use that same outline. Optional level markers appear above buildings; tile descriptions always include
levels for screen readers. Cursor variants are static SVGs with embedded image data and a dark edge,
so colour changes need no canvas, WebGL or server rendering.

A small sound bank plays locally served MP3 clips after a pointer or keyboard interaction. Turning
sound off stops current playback as well as future cues. Moves, placements, builds and match results
are sounded only after a confirmed state change. Initial snapshots, older versions and reconnects
are silent. Background tabs do not play cues; playback failures never interrupt the game. The server
and WebSocket protocol do not carry audio data, and settings add no match-state payload.

## Deployment decisions and limits

One Spring Boot process holds rooms and guest reservations. A SQL store persists accounts and account
sessions only. H2 uses a local file (a named volume in Compose); MySQL can use the existing shared
Lightsail database service through an external Docker network. The web container serves static assets
and proxies both `/api/` and `/ws`; the Java port is internal.

TLS ends at the host reverse proxy. Public deployments use an exact allowed origin and Secure account
cookies. The host proxy must overwrite untrusted forwarding headers before passing requests on. The
private run notes describe configuration, persistence and backups without placing credentials in Git.

Rooms expire after 10 idle minutes while waiting or five after finishing. Disconnected seats last for
the reconnect window. A silent network failure takes longer to detect than a normal close; heartbeats
bound that delay. Transport limits cap frames, message rates, buffered sends, connections and rooms.
The Compose memory limits are starting points rather than measured capacity claims.

The single-server name registry assumes one application instance. Multiple replicas would need a shared
reservation store, room ownership and routing; a load balancer alone would split matches. Online rooms
are not recovered after a process restart. Custom maps, Tag and local save files remain desktop features.
