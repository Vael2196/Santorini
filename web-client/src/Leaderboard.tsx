import { useEffect, useState } from "react";
import type { Ranking } from "./account";
interface Entry {
  position: number;
  name: string;
  rating: number;
  wins: number;
  losses: number;
  games: number;
}
export function Leaderboard({ profile }: { profile: Ranking | null }) {
  const [page, setPage] = useState(0),
    [entries, setEntries] = useState<Entry[]>([]),
    [error, setError] = useState(""),
    [loading, setLoading] = useState(true),
    [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    fetch(`/api/leaderboard?page=${page}`, {
      cache: "no-store",
      signal: controller.signal,
    })
      .then(async (r) => {
        if (!r.ok) throw Error("The leaderboard could not be loaded.");
        return r.json();
      })
      .then(setEntries)
      .catch((e) => {
        if (e.name !== "AbortError") setError(e.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [page, retry, profile?.games, profile?.rating]);
  return (
    <section className="paper-panel leaderboard" aria-label="Leaderboard">
      <span className="eyebrow">Ranked Classic duels</span>
      <h2>Leaderboard</h2>
      <p>
        Ratings begin at 1200. Beating a higher-rated opponent earns more
        points. Your first ten results are provisional.
      </p>
      {profile && (
        <p>
          Your rating: <strong>{profile.rating}</strong>
          {profile.provisional ? " · provisional" : ""} · {profile.wins} wins ·{" "}
          {profile.losses} losses
        </p>
      )}
      {loading ? (
        <p role="status">Loading standings…</p>
      ) : error ? (
        <p role="alert">
          {error}{" "}
          <button className="text-button" onClick={() => setRetry(retry + 1)}>
            Retry
          </button>
        </p>
      ) : (
        <>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Place</th>
                  <th>Player</th>
                  <th>Rating</th>
                  <th>Wins</th>
                  <th>Losses</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((e) => (
                  <tr key={e.position}>
                    <td>{e.position}</td>
                    <td>{e.name}</td>
                    <td>{e.rating}</td>
                    <td>{e.wins}</td>
                    <td>{e.losses}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!entries.length && <p>Play a ranked match to join the standings.</p>}
        </>
      )}
      <div className="leaderboard-pages">
        <button
          className="secondary"
          disabled={!page || loading}
          onClick={() => setPage(page - 1)}
        >
          Previous
        </button>
        <span>Page {page + 1}</span>
        <button
          className="secondary"
          disabled={entries.length < 50 || loading || page === 2000}
          onClick={() => setPage(page + 1)}
        >
          Next
        </button>
      </div>
    </section>
  );
}
