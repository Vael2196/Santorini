export const cursorColours = {
  brown: { label: "Dark brown", colour: "#543522" },
  charcoal: { label: "Charcoal", colour: "#303033" },
  teal: { label: "Teal", colour: "#176575" },
  ivory: { label: "Ivory", colour: "#fff6dd" },
} as const;
export type CursorColour = keyof typeof cursorColours;
export interface Preferences {
  cursorColour: CursorColour;
  showLevels: boolean;
  soundEnabled: boolean;
  volume: number;
}
export const defaultPreferences: Preferences = {
  cursorColour: "brown",
  showLevels: false,
  soundEnabled: true,
  volume: 80,
};
export const GUEST_PREFERENCES_KEY = "santorini.preferences.v1";
export function guestPreferences(): Preferences {
  try {
    const saved = JSON.parse(
      localStorage.getItem(GUEST_PREFERENCES_KEY) ?? "null",
    );
    if (
      saved &&
      Object.hasOwn(cursorColours, saved.cursorColour) &&
      typeof saved.showLevels === "boolean" &&
      typeof saved.soundEnabled === "boolean"
    )
      return {
        ...saved,
        volume:
          Number.isInteger(saved.volume) &&
          saved.volume >= 0 &&
          saved.volume <= 100
            ? saved.volume
            : 80,
      };
  } catch {
    /* Storage may be unavailable or contain an older value. */
  }
  return { ...defaultPreferences };
}
