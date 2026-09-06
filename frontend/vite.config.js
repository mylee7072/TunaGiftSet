import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import process from "node:process";

// Dev-time proxy avoids CORS handling entirely for local development: requests to
// /api are forwarded to the Spring Boot server, so the browser sees a single origin.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const apiTarget = env.VITE_DEV_API_TARGET || "http://127.0.0.1:8090";

  return {
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
          target: apiTarget,
          changeOrigin: true,
        },
      },
    },
    build: {
      rollupOptions: {
        output: {
          // React/ReactDOM/React Router rarely change between deploys — splitting
          // them into their own chunk means a browser that already cached them
          // only needs to re-download the (much smaller, more volatile) app
          // chunk on the next release, instead of re-fetching everything.
          manualChunks(id) {
            if (id.includes("node_modules/react-dom") || id.includes("node_modules/react/") || id.includes("node_modules/react-router")) {
              return "vendor";
            }
          },
        },
      },
    },
  };
});
