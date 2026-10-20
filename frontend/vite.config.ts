/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Vite + Vitest in one config. The dev server proxies /api to the local
// reference-service (Spring Boot on :8081); the same relative base is used
// in production behind the MIG-005 gateway (VITE_API_BASE can override).
//
// Service ports (from application.yml):
//   iam: 8080, reference: 8081, item: 8082, grievance: 8083,
//   nursing: 8093, adt: 8092, appointment: 8091, lab: 8094,
//   linen: 8085, equipment: 8088, diet: 8087, bloodbank: 8096,
//   billing: 8098, ordering: 8097, file: 8095, reports: 8106,
//   interop: 8105, queue: 8107, govt: 8108, forms: 8109,
//   cssd: 8102, chemotherapy: 8110, pharmacy: 8089
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
    },
  },
  build: {
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes("node_modules")) {
            if (
              id.includes("@chakra-ui") ||
              id.includes("@emotion") ||
              id.includes("@ark-ui") ||
              id.includes("@zag-js")
            ) {
              return "vendor-chakra";
            }
            if (id.includes("@tanstack")) {
              return "vendor-query";
            }
            if (
              id.includes("react-hook-form") ||
              id.includes("@hookform") ||
              id.includes("zod")
            ) {
              return "vendor-forms";
            }
            if (
              id.includes("react") ||
              id.includes("react-dom") ||
              id.includes("react-router") ||
              id.includes("scheduler")
            ) {
              return "vendor-react";
            }
          }
        },
      },
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    css: false,
  },
});
