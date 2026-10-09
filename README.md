# Santorini

A strategy game for two players: move your workers, build towers and reach the third level.

Santorini is a Java board-game project with a browser client and online multiplayer. Two players
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
between visits and server restarts. These stable identities provide a starting point for a future
leaderboard and points system; neither is implemented yet.

Private rooms admit players with an eight-character code. The public queue pairs players using
the same rules and never selects a private room. Each player chooses a power, marks themselves
ready, and places two workers. The server then controls the turn sequence and legal actions.

The browser view uses parchment textures, isometric stone tiles, framed panels and small tool
cursors. A hammer marks a legal build; a boot marks a legal move. Numbered workers, coordinates, available actions
and optional tower-level markers identify the board state without relying on colour alone. Legal tiles
support keyboard navigation and activation. The board has a dark outline for contrast. Game settings
offer four cursor colours (dark brown by default) and optional level markers above buildings. Short
interaction sounds share one mute button in the header. Settings are saved to an account for registered
players and on the device for guests.

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
| SQL account store | Persistent account IDs, password hashes and revocable session records |
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
boundaries. Local run and deployment notes live in `RUNNING.local.md`, which is deliberately ignored
by Git. They are supplied alongside the development patch and are not part of the public repository.

## Verification and current boundaries

Automated checks cover turn ownership, illegal and stale actions, god powers, matchmaking isolation,
concurrent name reservations, account authentication, CSRF protection, reconnection and desktop/browser
protocol compatibility. Browser checks cover the guest gate, accounts, isometric interactions and
responsive layout.

Accounts survive a restart. Rooms and matches are held in memory and do not. Rankings, points,
spectators, password recovery and saved online matches are future work. Custom maps and Tag modes
remain local. The deployment currently assumes one game-server instance.

## Project history and artwork

The original application was developed by Team024, including Vadim Filyakin (`vfil0001`). Its later
work includes Triton, save snapshots, a map editor and a Tag-inspired mode. The historical executable
in the repository predates online play; the current Maven desktop build contains the network client.

Selected artwork and sound licence files are kept in
[web-client/public/assets/licences](web-client/public/assets/licences).

[Santorini source](https://github.com/Vael2196/Santorini)
