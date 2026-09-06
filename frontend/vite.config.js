import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Dev-time proxy avoids CORS handling entirely for local development: requests to
// /api are forwarded to the Spring Boot server, so the browser sees a single origin.
export default defineConfig({
  plugins: [react()],
  test: {
    environment: "jsdom",
    setupFiles: "./src/test/setup.js",
    globals: true,
  },
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
