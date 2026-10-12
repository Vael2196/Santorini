# Santorini

A strategy game for two to four players: move your workers, build towers and reach the third level.

Santorini is a Java board-game project with a browser client and online multiplayer. Players
can meet through the public queue or share a private room code. A browser player and a Java
desktop player use the same server and can play at the same table.

The project began as a university team application built with Swing. This version extends that
work with a headless game engine, a Spring Boot server and a React/TypeScript interface. The
original local games, save snapshots and experimental map editor remain in the desktop application.

## Playing online

Guest players choose a temporary name and enter without a password. Names are unique across
active guests and registered accounts, ignoring case and surrounding spaces. A disconnected guest
has a short window to return before the name becomes available again.

Players can also create an account with a name and password. Account names and identities persist
between visits and server restarts. Logged-in players can join ranked Classic duels and appear on the leaderboard. Ratings begin at
1200 and use Elo: an upset earns more points than a win against a lower-rated opponent. The queue
favours similar ratings and gradually broadens its search. The first ten results are provisional.

Private rooms admit players with an eight-character code. The public queue pairs players using
the same rules and never selects a private room. Players choose a mode and rules, then mark themselves
ready and place their workers. Two- and three-player games give each player two workers. Four-player
games use one worker each, either in a free for all or two teams (seats 1/3 against 2/4). Four-player
games use Classic rules. A blocked player leaves a free-for-all game; a team win or forfeit settles
the whole team. A private practice mode adds a computer opponent without awarding ranking points. The server then controls the turn sequence and legal actions.

The browser view uses parchment textures, isometric stone tiles, framed panels and small tool
cursors. A hammer marks a legal build; a boot marks a legal move. Numbered workers, coordinates, available actions
and optional tower-level markers identify the board state without relying on colour alone. Legal tiles
support keyboard navigation and activation. The board has a dark outline for contrast. Game settings
offer four cursor colours (dark brown by default) and optional level markers above buildings. The self-hosted Kalam handwriting font keeps the parchment legible. Short
interaction sounds share one mute button and a volume popup with a slider and ± controls in the header. Settings are saved to an account for registered
players and on the device for guests.

Room chat accepts plain text only, with length and rate limits, link blocking and a basic word filter.
Messages belong to the room and disappear when it expires. Players can hide chat locally.

## Rules in this version

On a normal turn, select a worker, move to an empty neighbouring tile, then build nearby. Climbing
from level two to level three wins the match. A player with no legal move loses. A dome closes a tile.

| Rules or power | Behaviour |
| --- | --- |
| Classic | One move, followed by one build |
| Artemis | An optional second move, without returning to the starting tile |
| Demeter | An optional second build, on a different tile |
| Triton | This project's variant: additional moves may continue at the same height |

The online engine preserves the original project's one-level limit for both climbing and descending.
These are project variants; the implementation does not claim exact compatibility with the published
game's full rules or card catalogue. Optional actions have explicit buttons so a player can stop
moving or building without making an invalid move.

## Architecture

| Component | Responsibility |
| --- | --- |
| Java game core | Board entities, god strategies, turn transitions and immutable protocol records |
| Spring Boot server | Authoritative matches, rooms, matchmaking, reconnection and account sessions |
| React + TypeScript client | Browser controls and an SVG board using static image assets |
| Java Swing client | Original local game and a network view using the shared protocol |
| SQL account store | Persistent account IDs, password hashes revocable session records, preferences and ranked results |
| Nginx | Production static files and HTTP/WebSocket reverse proxy |

Clients send intentions rather than replacement boards. The server checks the player's seat, turn,
room version and legal actions before publishing a new snapshot. Games pause briefly when a player
disconnects. A returning player receives the current board; commands are not silently replayed.

Accounts use BCrypt password hashes, opaque session cookies and CSRF protection. Account cookies
are HttpOnly; session secrets are hashed in the database. Guest tokens stay out of public snapshots
and URLs. Registered names share a namespace with guest reservations.

A single Spring Boot service and a static web client keep deployment suitable for a modest server.
The default account database is a persistent H2 file. MySQL is supported through configuration,
including an existing shared database service. Flyway manages the schema. The browser board uses
SVG rather than a WebGL renderer, so the artwork adds no rendering work to the server.

See [the architecture notes](docs/ONLINE.md) for the protocol, account lifecycle and implementation
boundaries. Local run and deployment notes live in `RUNNING.local.md` and `LIGHTSAIL.local.md`, which is deliberately ignored
by Git. They are supplied alongside the development patch and are not part of the public repository.

## Verification and current boundaries

Automated checks cover turn ownership, illegal and stale actions, god powers, matchmaking isolation,
concurrent name reservations, account authentication, CSRF protection, reconnection and desktop/browser
protocol compatibility. Browser checks cover the guest gate, accounts, isometric interactions and
responsive layout.

Accounts, preferences and completed ranked results survive a restart. Rooms and matches are held
in memory; unfinished ranked games are voided after a restart. Spectators, password recovery and
saved online matches remain future work. Custom maps and Tag modes
remain local. The deployment currently assumes one game-server instance.

A future ASP.NET Core service could serve match history and statistics from completed-match events.
Java should retain rule validation and rating authority. That service would need its own read model,
an outbox and idempotent event handling; this version adds no .NET runtime or message broker.

## Project history and artwork

The original application was developed by Team024, including Vadim Filyakin (`vfil0001`). Its later
work includes Triton, save snapshots, a map editor and a Tag-inspired mode. The historical executable
in the repository predates online play; the current Maven desktop build contains the network client.

Selected artwork and sound licence files are kept in
[web-client/public/assets/licences](web-client/public/assets/licences).

[Santorini source](https://github.com/Vael2196/Santorini)
