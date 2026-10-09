import { useRef, useState } from "react";
import { accountRequest } from "./account";
import {
  GUEST_PREFERENCES_KEY,
  guestPreferences,
  defaultPreferences,
  type Preferences,
} from "./preferences";

export function usePreferences() {
  const [preferences, setPreferences] = useState(guestPreferences);
  const current = useRef(preferences);
  const owner = useRef<string | null>(null);
  const revision = useRef(0);
  const queue = useRef<Promise<void>>(Promise.resolve());
  const [saveState, setSaveState] = useState("");
  const [saveError, setSaveError] = useState("");

  function hydrate(accountId: string | null, saved?: Preferences) {
    owner.current = accountId;
    revision.current++;
    current.current = accountId
      ? { ...(saved ?? defaultPreferences) }
      : guestPreferences();
    setPreferences(current.current);
    setSaveState("");
    setSaveError("");
  }
  function update(change: Partial<Preferences>) {
    const next = { ...current.current, ...change };
    current.current = next;
    setPreferences(next);
    setSaveError("");
    const accountId = owner.current;
    const version = ++revision.current;
    if (!accountId) {
      try {
        localStorage.setItem(GUEST_PREFERENCES_KEY, JSON.stringify(next));
        setSaveState("Saved on this device");
      } catch {
        setSaveState("");
        setSaveError("These settings could not be saved on this device.");
      }
      return;
    }
    setSaveState("Saving…");
    // Serialise requests so a slow earlier response cannot replace a newer choice.
    queue.current = queue.current.then(async () => {
      if (owner.current !== accountId) return;
      try {
        await accountRequest("preferences", next);
        if (owner.current === accountId && revision.current === version)
          setSaveState("Saved to your account");
      } catch {
        if (owner.current === accountId && revision.current === version) {
          setSaveState("");
          setSaveError(
            "Your settings have changed here, but could not be saved to your account.",
          );
        }
      }
    });
  }
  return {
    preferences,
    hydrate,
    update,
    saveState,
    saveError,
    flush: () => queue.current,
  };
}
