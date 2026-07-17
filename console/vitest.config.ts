import vue from "@vitejs/plugin-vue";
import { resolve } from "node:path";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: [
      {
        find: "@",
        replacement: resolve(import.meta.dirname, "src"),
      },
      {
        find: /^~icons\/.*$/,
        replacement: resolve(import.meta.dirname, "tests/IconStub.ts"),
      },
    ],
  },
  test: {
    clearMocks: true,
    environment: "happy-dom",
    include: ["tests/**/*.test.ts"],
    restoreMocks: true,
  },
});
