import type { Preferences } from "./preferences";
export interface Account {
  id: string;
  name: string;
}
export interface Ranking {
  rating: number;
  wins: number;
  losses: number;
  games: number;
  provisional: boolean;
}
export interface AuthResult {
  ranking?: Ranking;
  authenticated: boolean;
  account?: Account;
  preferences?: Preferences;
}

export async function accountRequest(
  path: string,
  body?: unknown,
): Promise<AuthResult> {
  const headers: Record<string, string> = {};
  if (body !== undefined) {
    const csrf = await fetch("/api/auth/csrf", {
      credentials: "same-origin",
      cache: "no-store",
    });
    if (!csrf.ok)
      throw new Error(
        "Account services are unavailable. Try again in a moment.",
      );
    headers["X-XSRF-TOKEN"] = (await csrf.json()).token;
    headers["Content-Type"] = "application/json";
  }
  const response = await fetch(`/api/auth/${path}`, {
    method: body === undefined ? "GET" : "POST",
    headers,
    credentials: "same-origin",
    cache: "no-store",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const result = await response.json();
  if (!response.ok)
    throw new Error(
      result.message ?? "The request could not be completed. Please try again.",
    );
  return result;
}
