import { useEffect, useRef, useState } from "react";
export function VolumeControl({
  value,
  onChange,
  disabled,
}: {
  value: number;
  onChange: (value: number) => void;
  disabled: boolean;
}) {
  const [open, setOpen] = useState(false);
  const root = useRef<HTMLDivElement>(null),
    button = useRef<HTMLButtonElement>(null),
    slider = useRef<HTMLInputElement>(null);
  useEffect(() => {
    if (!open) return;
    slider.current?.focus();
    const outside = (e: PointerEvent) => {
      if (!root.current?.contains(e.target as Node)) setOpen(false);
    };
    const escape = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setOpen(false);
        button.current?.focus();
      }
    };
    document.addEventListener("pointerdown", outside);
    document.addEventListener("keydown", escape);
    return () => {
      document.removeEventListener("pointerdown", outside);
      document.removeEventListener("keydown", escape);
    };
  }, [open]);
  return (
    <div className="volume-control" ref={root}>
      <button
        ref={button}
        className="text-button"
        aria-label="Sound volume"
        aria-expanded={open}
        disabled={disabled}
        onClick={() => setOpen(!open)}
      >
        Volume
      </button>
      {open && (
        <div
          className="paper-panel volume-popup"
          role="dialog"
          aria-label="Sound volume"
        >
          <label htmlFor="sound-volume">
            Sound volume <strong>{value}%</strong>
          </label>
          <div className="volume-row">
            <button
              aria-label="Decrease volume"
              disabled={disabled || value === 0}
              onClick={() => onChange(Math.max(0, value - 5))}
            >
              −
            </button>
            <input
              ref={slider}
              id="sound-volume"
              type="range"
              min="0"
              max="100"
              step="1"
              value={value}
              disabled={disabled}
              onChange={(e) => onChange(Number(e.target.value))}
            />
            <button
              aria-label="Increase volume"
              disabled={disabled || value === 100}
              onClick={() => onChange(Math.min(100, value + 5))}
            >
              +
            </button>
          </div>
          <p className="subtle">Mute keeps your volume setting.</p>
        </div>
      )}
    </div>
  );
}
