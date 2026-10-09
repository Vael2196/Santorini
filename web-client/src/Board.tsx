import { useId } from "react";
import type { Cell, GameAction, RoomState } from "./types";

export const emptyCells = (): Cell[] =>
  Array.from({ length: 25 }, (_, i) => ({
    row: Math.floor(i / 5),
    col: i % 5,
    level: 0,
    dome: false,
    worker: null,
    owner: null,
  }));
export function cellAction(cell: Cell, actions: GameAction[]) {
  return actions.find((a) =>
    a.type === "SELECT"
      ? cell.worker !== null && a.worker === cell.worker
      : a.row === cell.row && a.col === cell.col,
  );
}
const diamond = "0,-27.5 55,0 0,27.5 -55,0";

function Worker({
  owner,
  number,
  y,
}: {
  owner: number;
  number: number;
  y: number;
}) {
  return (
    <g
      transform={`translate(0 ${y})`}
      className={`piece piece-${owner}`}
      aria-hidden="true"
    >
      <ellipse cy="5" rx="12" ry="6" fill="#30241e" opacity=".2" />
      <path
        d="M-10 0 L-8-20 Q0-27 8-20 L10 0 Q0 8-10 0Z"
        fill={owner === 0 ? "#a34e36" : "#39666d"}
        stroke="#f3dfb4"
        strokeWidth="1.5"
      />
      <ellipse
        cy="-25"
        rx="6.5"
        ry="6.5"
        fill={owner === 0 ? "#c66a46" : "#528790"}
        stroke="#eed8b2"
        strokeWidth="1.3"
      />
      <text
        y="-7"
        textAnchor="middle"
        fill="#fff5df"
        fontSize="9"
        fontFamily="sans-serif"
        fontWeight="700"
      >
        {number}
      </text>
    </g>
  );
}
function Tile({
  cell,
  action,
  selected,
  label,
  onAction,
}: {
  cell: Cell;
  action?: GameAction;
  selected: boolean;
  label: string;
  onAction?: (action: GameAction) => void;
}) {
  const x = 410 + (cell.col - cell.row) * 55;
  const y = 138 + (cell.row + cell.col) * 27.5;
  const height = cell.level * 15;
  const cursor = action
    ? ({ BUILD: "build", MOVE: "move", PLACE: "place", SELECT: "select" }[
        action.type
      ] ?? "select")
    : "";
  return (
    <g
      transform={`translate(${x} ${y})`}
      className={`board-cell ${action ? "legal" : ""} ${selected ? "selected" : ""} cursor-${cursor}`}
    >
      <g className="tile-art" aria-hidden="true" pointerEvents="none">
        <svg
          x="-55"
          y="-27.5"
          width="110"
          height="60"
          viewBox="38 395 180 98"
          preserveAspectRatio="none"
        >
          <image href="/assets/stone-tile.png" width="256" height="512" />
        </svg>
        {Array.from({ length: cell.level }, (_, level) => (
          <g key={level} transform={`translate(0 ${-15 * (level + 1)})`}>
            <path
              d="M-30 0 L0 15 L30 0 V15 L0 30 L-30 15Z"
              fill="#cfb58a"
              stroke="#957953"
              strokeWidth=".7"
            />
            <path d="M0 15 L30 0 V15 L0 30Z" fill="#bfa176" />
            <svg
              x="-30"
              y="-15"
              width="60"
              height="36"
              viewBox="38 382 180 110"
              preserveAspectRatio="none"
            >
              <image href="/assets/stone-block.png" width="256" height="512" />
            </svg>
            <path d="M-8 11 V21 M14 8 V18" stroke="#ac8e65" strokeWidth=".8" />
          </g>
        ))}
        {cell.dome && (
          <g transform={`translate(0 ${-height})`}>
            <ellipse cy="-4" rx="23" ry="11" fill="#315d70" />
            <path
              d="M-23-5 A23 27 0 0 1 23-5 Q0 7-23-5Z"
              fill="#6595a1"
              stroke="#345d67"
              strokeWidth="1"
            />
            <path
              d="M-15-12 Q-10-27 0-28"
              fill="none"
              stroke="#a5c4c5"
              strokeWidth="3"
              strokeLinecap="round"
            />
          </g>
        )}
        <polygon
          opacity={action ? 0.8 : 0}
          className="legal-outline"
          points={diamond}
          fill="none"
          stroke="#896636"
          strokeWidth="2"
        />
        {selected && (
          <ellipse
            cy={-height + 3}
            rx="18"
            ry="9"
            fill="none"
            stroke="#ac653a"
            strokeWidth="2"
          />
        )}
        {cell.owner !== null && (
          <Worker
            owner={cell.owner}
            number={(cell.worker! % 2) + 1}
            y={-height - 4}
          />
        )}
        <text className="coordinate" x="0" y="22" textAnchor="middle">
          {String.fromCharCode(65 + cell.col)}
          {cell.row + 1}
        </text>
        {cell.level > 0 && (
          <text className="height-label" x="35" y="11" textAnchor="middle">
            {cell.dome ? "D" : cell.level}
          </text>
        )}
      </g>
      <polygon
        fill="none"
        stroke="none"
        pointerEvents="all"
        className="tile-hit"
        points={diamond}
        role="button"
        tabIndex={action ? 0 : -1}
        data-testid={`cell-${cell.row}-${cell.col}`}
        aria-label={label}
        aria-disabled={!action}
        onClick={() => action && onAction?.(action)}
        onKeyDown={(event) => {
          if (action && (event.key === "Enter" || event.key === " ")) {
            event.preventDefault();
            onAction?.(action);
          }
        }}
      />
    </g>
  );
}

export function Board({
  room,
  actions = [],
  onAction,
  illustration = false,
}: {
  room?: RoomState;
  actions?: GameAction[];
  onAction?: (action: GameAction) => void;
  illustration?: boolean;
}) {
  const id = useId().replace(/:/g, "");
  const cells = room?.game?.cells ?? emptyCells();
  const displayCells = illustration
    ? cells.map((cell) => {
        const key = `${cell.row},${cell.col}`;
        const towers: Record<string, number> = {
          "1,2": 3,
          "2,1": 1,
          "2,3": 2,
          "3,2": 1,
        };
        return {
          ...cell,
          level: towers[key] ?? 0,
          dome: key === "1,2",
          owner: key === "2,1" ? 0 : key === "3,3" ? 1 : null,
          worker: key === "2,1" ? 0 : key === "3,3" ? 2 : null,
        };
      })
    : cells;
  return (
    <div
      className={`isometric-board ${illustration ? "illustration" : ""}`}
      role={illustration ? undefined : "group"}
      aria-label={illustration ? undefined : "Santorini board"}
      aria-hidden={illustration || undefined}
    >
      <svg
        viewBox="80 45 660 450"
        className="board-canvas"
        aria-labelledby={illustration ? undefined : `${id}-title`}
      >
        {!illustration && (
          <title id={`${id}-title`}>
            The island: five rows and five columns. Highlighted tiles are
            available.
          </title>
        )}
        <defs>
          <filter
            id={`${id}-shadow`}
            x="-30%"
            y="-30%"
            width="160%"
            height="170%"
          >
            <feDropShadow
              dx="0"
              dy="14"
              stdDeviation="10"
              floodColor="#61472d"
              floodOpacity=".25"
            />
          </filter>
        </defs>
        <g
          className="island-foundation"
          aria-hidden="true"
          filter={`url(#${id}-shadow)`}
        >
          <path
            d="M410 101 L690 241 L690 277 L410 417 L130 277 L130 241Z"
            fill="#a98c62"
            stroke="#806545"
            strokeWidth="1"
          />
          <path d="M410 381 L690 241 V277 L410 417Z" fill="#94744f" />
          <path
            d="M410 101 L690 241 L410 381 L130 241Z"
            fill="#d4bc94"
            stroke="#b1946c"
            strokeWidth="3"
          />
          <path
            d="M147 261 L410 393 L673 261 M147 271 L410 403 L673 271"
            fill="none"
            stroke="#6e5339"
            strokeOpacity=".3"
          />
        </g>
        {Array.from({ length: 5 }, (_, i) => (
          <g key={i} aria-hidden="true" className="axis-label">
            <text
              x={410 + (i - 5) * 55}
              y={138 + (i + 5) * 27.5 + 18}
              textAnchor="middle"
            >
              {String.fromCharCode(65 + i)}
            </text>
            <text
              x={410 + (5 - i) * 55}
              y={138 + (i + 5) * 27.5 + 18}
              textAnchor="middle"
            >
              {i + 1}
            </text>
          </g>
        ))}
        {[...displayCells]
          .sort((a, b) => a.row + a.col - (b.row + b.col) || a.row - b.row)
          .map((cell) => (
            <Tile
              key={`${cell.row}-${cell.col}`}
              cell={cell}
              action={illustration ? undefined : cellAction(cell, actions)}
              selected={
                cell.worker !== null &&
                cell.worker === room?.game?.selectedWorker
              }
              label={`${String.fromCharCode(65 + cell.col)}${cell.row + 1}, level ${cell.level}${cell.dome ? ", dome" : ""}${cell.owner !== null ? `, ${room?.players[cell.owner]?.name ?? "player"}'s worker` : ""}`}
              onAction={onAction}
            />
          ))}
      </svg>
    </div>
  );
}
