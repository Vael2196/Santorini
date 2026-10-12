export interface GameAction {
  type: string;
  worker: number | null;
  row: number | null;
  col: number | null;
}
export interface Cell {
  row: number;
  col: number;
  level: number;
  dome: boolean;
  worker: number | null;
  owner: number | null;
}
export interface MatchSnapshot {
  phase: string;
  turn: number;
  selectedWorker: number | null;
  winner: number | null;
  workersPerPlayer?: number;
  eliminated?: number[];
  winners?: number[];
  cells: Cell[];
  legalActions: GameAction[];
}
export interface Seat {
  name: string;
  god: string;
  ready: boolean;
  connected: boolean;
  bot?: boolean;
  team?: number;
  rating?: number | null;
}
export interface RoomState {
  id: string;
  publicRoom: boolean;
  mode?: string;
  capacity?: number;
  ranked?: boolean;
  matchId?: string;
  ratingChange?: { before: number; after: number; delta: number } | null;
  rules: string;
  status: string;
  version: number;
  players: Seat[];
  yourSeat: number;
  paused: boolean;
  reconnectDeadline: number;
  game: MatchSnapshot | null;
  result: string | null;
}
export interface Identity {
  id: string | null;
  name: string;
  guest: boolean;
}
export interface ChatMessage {
  id: string;
  seat: number;
  name: string;
  text: string;
  sentAt: number;
}
export interface ServerEvent {
  requestId?: string;
  roomId?: string;
  chat?: ChatMessage;
  history?: ChatMessage[];
  identity: Identity | null;
  type: string;
  token: string | null;
  room: RoomState | null;
  code: string | null;
  message: string | null;
}
export interface Command {
  type: string;
  rules?: string;
  mode?: string;
  ranked?: boolean;
  text?: string;
  roomId?: string;
  god?: string;
  action?: GameAction;
  version?: number;
}
