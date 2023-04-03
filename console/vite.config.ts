import { fileURLToPath, URL } from "url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import vueJsx from "@vitejs/plugin-vue-jsx";
import Icons from "unplugin-icons/vite";

// https://vitejs.dev/config/
export default ({ mode }: { mode: string }) => {
  const isProduction = mode === "production";
  const outDir = isProduction
    ? "../src/main/resources/console"
    : "../build/resources/main/console";

  return defineConfig({
    plugins: [vue(), vueJsx(), Icons({ compiler: "vue3" })],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
      },
    },
    build: {
      outDir,
      emptyOutDir: true,
      lib: {
        entry: "src/index.ts",
        name: "PluginMoments",
        formats: ["iife"],
        fileName: () => "main.js",
      },
      rollupOptions: {
        external: [
          "vue",
          "vue-router",
          "@halo-dev/shared",
          "@halo-dev/components",
        ],
        output: {
          globals: {
            vue: "Vue",
            "vue-router": "VueRouter",
            "@halo-dev/components": "HaloComponents",
            "@halo-dev/console-shared": "HaloConsoleShared",
          },
        },
      },
    },
  });
};
