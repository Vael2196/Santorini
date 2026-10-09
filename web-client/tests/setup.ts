import "@testing-library/jest-dom/vitest";
import WebSocket from "ws";
import { afterEach } from "vitest";
// Render the real React view while HTTP and sockets reach the real Spring server.
// The small cookie jar stands in for browser transport, including HttpOnly cookies.
const cookies = new Map<string, string>();
const nativeFetch = globalThis.fetch;
function cookieHeader() {
  return [...cookies].map(([key, value]) => `${key}=${value}`).join("; ");
}
globalThis.fetch = async (input, init) => {
  const url = new URL(String(input), "http://127.0.0.1:8080");
  url.host = "127.0.0.1:8080";
  const headers = new Headers(init?.headers);
  if (cookies.size) headers.set("Cookie", cookieHeader());
  const response = await nativeFetch(url, { ...init, headers });
  for (const cookie of response.headers.getSetCookie()) {
    const [pair] = cookie.split(";");
    const index = pair.indexOf("=");
    const key = pair.slice(0, index),
      value = pair.slice(index + 1);
    if (!value || /Max-Age=0/i.test(cookie)) cookies.delete(key);
    else cookies.set(key, value);
  }
  return response;
};
class ServerSocket extends WebSocket {
  constructor(url: string | URL) {
    super(String(url).replace(":5173", ":8080"), {
      origin: "http://localhost:5173",
      headers: { Cookie: cookieHeader() },
    });
  }
}
Object.defineProperty(globalThis, "WebSocket", {
  value: ServerSocket,
  configurable: true,
});
afterEach(() => cookies.clear());
