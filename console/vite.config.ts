import { fileURLToPath, URL } from "url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import Icons from "unplugin-icons/vite";

// https://vitejs.dev/config/
export default ({ mode }: { mode: string }) => {
  const isProduction = mode === "production";
  const outDir = isProduction
    ? "../src/main/resources/console"
    : "../build/resources/main/console";

  return defineConfig({
    plugins: [vue(), Icons({ compiler: "vue3" })],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
        // To resolve conflict between @tiptap and @halo-dev/richtext-editor
        "@tiptap/pm/state": "@halo-dev/richtext-editor",
        "@tiptap/core": "@halo-dev/richtext-editor",
        "@tiptap/pm/view": "@halo-dev/richtext-editor",
      },
    },
    define: {
      "process.env": process.env,
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
          "@vueuse/core",
          "@vueuse/components",
          "@vueuse/router",
          "@halo-dev/shared",
          "@halo-dev/components",
          "@halo-dev/richtext-editor",
        ],
        output: {
          globals: {
            vue: "Vue",
            "vue-router": "VueRouter",
            "@vueuse/core": "VueUse",
            "@vueuse/components": "VueUse",
            "@vueuse/router": "VueUse",
            "@halo-dev/console-shared": "HaloConsoleShared",
            "@halo-dev/components": "HaloComponents",
            "@halo-dev/richtext-editor": "RichTextEditor",
          },
        },
      },
    },
  });
};
