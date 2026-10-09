import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/ws": { target: "ws://127.0.0.1:8080", ws: true },
      "/actuator": "http://127.0.0.1:8080",
      "/api": "http://127.0.0.1:8080",
    },
  },
});
