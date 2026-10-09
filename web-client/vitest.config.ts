import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    environmentOptions: { jsdom: { url: "http://localhost:5173" } },
    include: ["tests/**/*.integration.tsx"],
    testTimeout: 15_000,
    setupFiles: ["./tests/setup.ts"],
  },
});
