import {
  cursorColours,
  type CursorColour,
  type Preferences,
} from "./preferences";

export function Settings({
  preferences,
  onChange,
  disabled,
  account,
  saveState,
  saveError,
}: {
  preferences: Preferences;
  onChange: (change: Partial<Preferences>) => void;
  disabled: boolean;
  account: boolean;
  saveState: string;
  saveError: string;
}) {
  return (
    <section className="paper-panel settings-panel" aria-label="Game settings">
      <div>
        <span className="eyebrow">Game settings</span>
        <h2>Board and controls</h2>
        <p>
          {account
            ? "Settings are saved to your account."
            : "Guest settings are saved on this device."}
        </p>
      </div>
      <div className="settings-controls">
        <div>
          <label htmlFor="cursor-colour">In-game cursor colour</label>
          <select
            id="cursor-colour"
            value={preferences.cursorColour}
            disabled={disabled}
            onChange={(event) =>
              onChange({ cursorColour: event.target.value as CursorColour })
            }
          >
            {Object.entries(cursorColours).map(([value, colour]) => (
              <option key={value} value={value}>
                {colour.label}
              </option>
            ))}
          </select>
          <p className="subtle cursor-preview">
            <img
              src={`/assets/cursors/${preferences.cursorColour}/hammer.svg`}
              alt=""
            />{" "}
            Preview · building cursor
          </p>
        </div>
        <div>
          <label className="setting-checkbox">
            <input
              type="checkbox"
              checked={preferences.showLevels}
              disabled={disabled}
              onChange={(event) =>
                onChange({ showLevels: event.target.checked })
              }
            />
            Show tower levels
          </label>
          <p className="subtle">
            Display a level marker above each building. D marks a dome.
          </p>
          <p className="subtle">
            All game sounds use the sound button in the header.
          </p>
        </div>
      </div>
      <p className="settings-status" aria-live="polite">
        {saveState}
      </p>
      {saveError && (
        <div className="settings-error" role="alert">
          {saveError}{" "}
          <button
            className="text-button"
            disabled={disabled}
            onClick={() => onChange({})}
          >
            Retry saving
          </button>
        </div>
      )}
    </section>
  );
}
